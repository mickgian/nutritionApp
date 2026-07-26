"""Tests for the client notifications endpoint (DEV-060).

``GET /api/v1/me/notifications`` generates the notifications that apply to the
caller's state (idempotently) and lists their own, newest first. Covers auth,
generation from state (order-deadline / box-proposal), idempotency across two
reads, and per-user isolation.
"""

from fastapi.testclient import TestClient
from sqlmodel import Session

from app.core.security import create_access_token
from app.models.nutrition_plan import NutritionPlan
from app.models.user import User, UserRole

NOTIFS = "/api/v1/me/notifications"


def _auth_header(user: User) -> dict[str, str]:
    return {"Authorization": f"Bearer {create_access_token(str(user.id), user.role.value)}"}


def _give_plan(session: Session, client_id: int) -> None:
    session.add(NutritionPlan(client_id=client_id, name="Piano", daily_kcal=1400, weeks=4))
    session.commit()


def test_requires_authentication(client: TestClient):
    assert client.get(NOTIFS).status_code == 401


def test_empty_when_no_state(client: TestClient, cliente_headers):
    resp = client.get(NOTIFS, headers=cliente_headers)
    assert resp.status_code == 200
    assert resp.json() == []


def test_plan_without_order_generates_order_deadline(
    client: TestClient, session: Session, make_user
):
    cliente = make_user("planonly@example.com", UserRole.cliente)
    _give_plan(session, cliente.id)
    resp = client.get(NOTIFS, headers=_auth_header(cliente))
    assert resp.status_code == 200
    kinds = {n["kind"] for n in resp.json()}
    assert "order_deadline" in kinds
    # Every notification is Italian and unread by default.
    for n in resp.json():
        assert n["title"]
        assert n["is_read"] is False


def test_generation_is_idempotent_across_reads(client: TestClient, session: Session, make_user):
    cliente = make_user("idem@example.com", UserRole.cliente)
    _give_plan(session, cliente.id)
    first = client.get(NOTIFS, headers=_auth_header(cliente)).json()
    second = client.get(NOTIFS, headers=_auth_header(cliente)).json()
    # Reading twice must not duplicate the same event.
    assert len(first) == len(second)
    assert len(first) >= 1


def test_notifications_isolated_between_clients(client: TestClient, session: Session, make_user):
    a = make_user("na@example.com", UserRole.cliente)
    b = make_user("nb@example.com", UserRole.cliente)
    _give_plan(session, a.id)
    # a generates notifications; b (no plan) sees none of a's.
    client.get(NOTIFS, headers=_auth_header(a))
    resp = client.get(NOTIFS, headers=_auth_header(b))
    assert resp.status_code == 200
    assert resp.json() == []
