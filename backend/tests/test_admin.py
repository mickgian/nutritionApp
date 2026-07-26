"""Admin panel endpoints (DEV-080): clients directory, box orders, promotions.

Every ``/admin/*`` route is studio-only: a ``cliente`` token must get 403, and
an admin token must see data across all clients. Promotions fan out one
notification per active client and land in each client's own feed.
"""

from fastapi.testclient import TestClient
from sqlmodel import Session

from app.core.security import create_access_token
from app.models.nutrition_plan import NutritionPlan
from app.models.order import Order, OrderFormula, OrderStatus, PickupSlot
from app.models.user import User


def _headers(user: User) -> dict[str, str]:
    token = create_access_token(subject=str(user.id), role=user.role.value)
    return {"Authorization": f"Bearer {token}"}


def _seed_order(session: Session, client_id: int, formula: OrderFormula) -> Order:
    plan = NutritionPlan(client_id=client_id, name="Dimagrimento 1400", daily_kcal=1400, weeks=4)
    session.add(plan)
    session.commit()
    session.refresh(plan)
    order = Order(
        client_id=client_id,
        plan_id=plan.id,
        formula=formula,
        pickup=PickupSlot.mattina,
        status=OrderStatus.paid,
        price_cents=8900,
    )
    session.add(order)
    session.commit()
    session.refresh(order)
    return order


# --------------------------------------------------------------------------- #
# GET /admin/clients                                                           #
# --------------------------------------------------------------------------- #


def test_list_clients_requires_admin(client: TestClient, cliente_headers):
    resp = client.get("/api/v1/admin/clients", headers=cliente_headers)
    assert resp.status_code == 403


def test_list_clients_requires_auth(client: TestClient):
    resp = client.get("/api/v1/admin/clients")
    assert resp.status_code == 401


def test_list_clients_returns_clients_with_active_plan(
    client: TestClient, session: Session, admin_headers, make_user
):
    c1 = make_user("mario@example.com")
    make_user("giulia@example.com")
    session.add(NutritionPlan(client_id=c1.id, name="Sportivo 2200", daily_kcal=2200, weeks=6))
    session.commit()

    resp = client.get("/api/v1/admin/clients", headers=admin_headers)

    assert resp.status_code == 200
    body = {c["email"]: c for c in resp.json()}
    # The admin herself is not a cliente and must not appear.
    assert "admin@studiomeridia.it" not in body
    assert body["mario@example.com"]["active_plan"] == "Sportivo 2200"
    assert body["giulia@example.com"]["active_plan"] is None


# --------------------------------------------------------------------------- #
# GET /admin/orders                                                            #
# --------------------------------------------------------------------------- #


def test_list_orders_requires_admin(client: TestClient, cliente_headers):
    resp = client.get("/api/v1/admin/orders", headers=cliente_headers)
    assert resp.status_code == 403


def test_list_orders_spans_all_clients_with_names(
    client: TestClient, session: Session, admin_headers, make_user
):
    c1 = make_user("mario@example.com")
    c2 = make_user("giulia@example.com")
    _seed_order(session, c1.id, OrderFormula.subscription)
    _seed_order(session, c2.id, OrderFormula.single)

    resp = client.get("/api/v1/admin/orders", headers=admin_headers)

    assert resp.status_code == 200
    orders = resp.json()
    assert len(orders) == 2
    emails = {o["client_email"] for o in orders}
    assert emails == {"mario@example.com", "giulia@example.com"}
    assert all(o["client_name"] for o in orders)


def test_list_orders_empty(client: TestClient, admin_headers):
    resp = client.get("/api/v1/admin/orders", headers=admin_headers)
    assert resp.status_code == 200
    assert resp.json() == []


# --------------------------------------------------------------------------- #
# POST /admin/promotions                                                       #
# --------------------------------------------------------------------------- #


def test_send_promotion_requires_admin(client: TestClient, cliente_headers):
    resp = client.post(
        "/api/v1/admin/promotions",
        headers=cliente_headers,
        json={"title": "Promo", "body": "Sconto"},
    )
    assert resp.status_code == 403


def test_send_promotion_fans_out_to_every_client(client: TestClient, admin_headers, make_user):
    a = make_user("anna@example.com")
    make_user("bruno@example.com")

    resp = client.post(
        "/api/v1/admin/promotions",
        headers=admin_headers,
        json={"title": "Promo di settembre", "body": "Porta un amico", "icon": "📣"},
    )

    assert resp.status_code == 201
    assert resp.json()["recipients"] == 2

    # The promotion lands in a client's own notification feed.
    feed = client.get("/api/v1/me/notifications", headers=_headers(a)).json()
    promo = next(n for n in feed if n["kind"] == "promotion")
    assert promo["title"] == "Promo di settembre"
    assert promo["icon"] == "📣"


def test_send_promotion_default_icon(client: TestClient, admin_headers, make_user):
    make_user("anna@example.com")
    resp = client.post(
        "/api/v1/admin/promotions",
        headers=admin_headers,
        json={"title": "Novità", "body": "Nuovo menu"},
    )
    assert resp.status_code == 201
    assert resp.json()["recipients"] == 1


def test_send_promotion_rejects_empty_title(client: TestClient, admin_headers):
    resp = client.post(
        "/api/v1/admin/promotions",
        headers=admin_headers,
        json={"title": "", "body": "Sconto"},
    )
    assert resp.status_code == 422


def test_send_promotion_with_no_clients_returns_zero(client: TestClient, admin_headers):
    resp = client.post(
        "/api/v1/admin/promotions",
        headers=admin_headers,
        json={"title": "Promo", "body": "Sconto"},
    )
    assert resp.status_code == 201
    assert resp.json()["recipients"] == 0


def test_promotion_isolated_between_clients(client: TestClient, admin_headers, make_user):
    """A promotion sent while a client exists must not leak to a later-only feed check."""
    seen = make_user("seen@example.com")
    client.post(
        "/api/v1/admin/promotions",
        headers=admin_headers,
        json={"title": "Promo", "body": "Sconto"},
    )
    # A brand-new client created after the broadcast has no promotion.
    fresh = make_user("fresh@example.com")
    fresh_feed = client.get("/api/v1/me/notifications", headers=_headers(fresh)).json()
    assert not any(n["kind"] == "promotion" for n in fresh_feed)
    # The original recipient still has exactly one.
    seen_feed = client.get("/api/v1/me/notifications", headers=_headers(seen)).json()
    assert sum(1 for n in seen_feed if n["kind"] == "promotion") == 1
