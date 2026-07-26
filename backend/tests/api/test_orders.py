"""Tests for meal-box orders (DEV-040).

A client places a box order (``POST /api/v1/orders``) and lists their own
(``GET /api/v1/orders``). Ordering requires an active plan (no plan → 403, an
expected condition, not a 500). Prices are server-authoritative: single €89,
subscription €79. Covers happy path (both formulas), the 403 no-plan guard,
422 validation, the duplicate-subscription 409 edge, and ownership isolation.
"""

from fastapi.testclient import TestClient
from sqlmodel import Session

from app.core.security import create_access_token
from app.models.nutrition_plan import NutritionPlan
from app.models.user import User, UserRole

ORDERS = "/api/v1/orders"


def _auth_header(user: User) -> dict[str, str]:
    token = create_access_token(subject=str(user.id), role=user.role.value)
    return {"Authorization": f"Bearer {token}"}


def _give_plan(session: Session, client_id: int) -> None:
    session.add(
        NutritionPlan(client_id=client_id, name="Dimagrimento 1400", daily_kcal=1400, weeks=4)
    )
    session.commit()


def _order_body(formula: str = "single", pickup: str = "mattina") -> dict:
    return {"formula": formula, "pickup": pickup}


def test_requires_authentication(client: TestClient):
    resp = client.post(ORDERS, json=_order_body())
    assert resp.status_code == 401


def test_order_without_plan_returns_403(client: TestClient, make_user):
    cliente = make_user("noplan@example.com", UserRole.cliente)
    resp = client.post(ORDERS, json=_order_body(), headers=_auth_header(cliente))
    assert resp.status_code == 403
    assert resp.json()["detail"]  # Italian message


def test_create_single_order(client: TestClient, session: Session, make_user):
    cliente = make_user("single@example.com", UserRole.cliente)
    _give_plan(session, cliente.id)
    resp = client.post(ORDERS, json=_order_body("single"), headers=_auth_header(cliente))
    assert resp.status_code == 201
    body = resp.json()
    assert body["formula"] == "single"
    assert body["pickup"] == "mattina"
    assert body["price_cents"] == 8900  # €89
    assert body["status"] == "pending_payment"


def test_create_subscription_order(client: TestClient, session: Session, make_user):
    cliente = make_user("sub@example.com", UserRole.cliente)
    _give_plan(session, cliente.id)
    resp = client.post(
        ORDERS, json=_order_body("subscription", "pomeriggio"), headers=_auth_header(cliente)
    )
    assert resp.status_code == 201
    body = resp.json()
    assert body["formula"] == "subscription"
    assert body["price_cents"] == 7900  # €79 per box


def test_invalid_formula_returns_422(client: TestClient, session: Session, make_user):
    cliente = make_user("badformula@example.com", UserRole.cliente)
    _give_plan(session, cliente.id)
    resp = client.post(ORDERS, json=_order_body("familia"), headers=_auth_header(cliente))
    assert resp.status_code == 422


def test_missing_pickup_returns_422(client: TestClient, session: Session, make_user):
    cliente = make_user("nopickup@example.com", UserRole.cliente)
    _give_plan(session, cliente.id)
    resp = client.post(ORDERS, json={"formula": "single"}, headers=_auth_header(cliente))
    assert resp.status_code == 422


def test_duplicate_active_subscription_returns_409(client: TestClient, session: Session, make_user):
    cliente = make_user("dupsub@example.com", UserRole.cliente)
    _give_plan(session, cliente.id)
    first = client.post(ORDERS, json=_order_body("subscription"), headers=_auth_header(cliente))
    assert first.status_code == 201
    second = client.post(ORDERS, json=_order_body("subscription"), headers=_auth_header(cliente))
    assert second.status_code == 409
    assert second.json()["detail"]  # Italian message


def test_single_order_allowed_alongside_subscription(
    client: TestClient, session: Session, make_user
):
    """The duplicate guard is subscription-only; a one-off box is still allowed."""
    cliente = make_user("mixed@example.com", UserRole.cliente)
    _give_plan(session, cliente.id)
    client.post(ORDERS, json=_order_body("subscription"), headers=_auth_header(cliente))
    resp = client.post(ORDERS, json=_order_body("single"), headers=_auth_header(cliente))
    assert resp.status_code == 201


def test_list_returns_own_orders(client: TestClient, session: Session, make_user):
    cliente = make_user("lister@example.com", UserRole.cliente)
    _give_plan(session, cliente.id)
    client.post(ORDERS, json=_order_body("single"), headers=_auth_header(cliente))
    resp = client.get(ORDERS, headers=_auth_header(cliente))
    assert resp.status_code == 200
    orders = resp.json()
    assert len(orders) == 1
    assert orders[0]["formula"] == "single"


def test_order_isolation_between_clients(client: TestClient, session: Session, make_user):
    a = make_user("owner-a@example.com", UserRole.cliente)
    b = make_user("owner-b@example.com", UserRole.cliente)
    _give_plan(session, a.id)
    _give_plan(session, b.id)
    client.post(ORDERS, json=_order_body("single"), headers=_auth_header(a))
    # b never sees a's order.
    resp = client.get(ORDERS, headers=_auth_header(b))
    assert resp.status_code == 200
    assert resp.json() == []
