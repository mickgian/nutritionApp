"""Tests for appointment reschedule / cancel + credits (DEV-050).

A client can move (`PATCH /appointments/{id}`) or cancel (`DELETE
/appointments/{id}`) their own appointment. Cancelling more than 48h before the
visit issues a 6-month store credit; within 48h the cancellation is allowed but
no credit is issued (server-enforced, not just UI). `GET /me/credits` lists the
client's own active credits. Covers the 48h boundary both sides, slot release,
reschedule conflicts, ownership isolation, and credit expiry.
"""

from datetime import UTC, datetime, timedelta

from fastapi.testclient import TestClient
from sqlmodel import Session

from app.core.security import create_access_token
from app.models.credit import Credit
from app.models.user import User, UserRole

APPTS = "/api/v1/appointments"
AVAIL = "/api/v1/availability"
ADMIN_AVAIL = "/api/v1/admin/availability"
CREDITS = "/api/v1/me/credits"


def _auth_header(user: User) -> dict[str, str]:
    return {"Authorization": f"Bearer {create_access_token(str(user.id), user.role.value)}"}


def _future_iso(days: int, hour: int = 10) -> str:
    return (
        (datetime.now(UTC) + timedelta(days=days))
        .replace(hour=hour, minute=0, second=0, microsecond=0)
        .isoformat()
    )


def _open_slot(client: TestClient, admin_headers, days: int) -> int:
    resp = client.put(
        ADMIN_AVAIL,
        json={"starts_at": _future_iso(days), "duration_min": 60, "is_open": True},
        headers=admin_headers,
    )
    assert resp.status_code == 200
    return resp.json()["id"]


def _book(client: TestClient, headers, slot_id: int, visit_type: str = "prima") -> dict:
    resp = client.post(APPTS, json={"slot_id": slot_id, "visit_type": visit_type}, headers=headers)
    assert resp.status_code == 201
    return resp.json()


def _range_query() -> str:
    start = datetime.now(UTC).date().isoformat()
    end = (datetime.now(UTC) + timedelta(days=60)).date().isoformat()
    return f"?from={start}&to={end}"


# --------------------------------- cancel --------------------------------- #


def test_cancel_requires_auth(client: TestClient):
    assert client.delete(f"{APPTS}/1").status_code == 401


def test_cancel_more_than_48h_issues_credit(client: TestClient, admin_headers, cliente_headers):
    slot_id = _open_slot(client, admin_headers, days=5)
    appt = _book(client, cliente_headers, slot_id)
    resp = client.delete(f"{APPTS}/{appt['id']}", headers=cliente_headers)
    assert resp.status_code == 200
    body = resp.json()
    assert body["status"] == "cancelled"
    assert body["credit"] is not None
    assert body["credit"]["amount_cents"] == 9000  # the prima price is refunded
    # The credit is listed as active.
    active = client.get(CREDITS, headers=cliente_headers).json()
    assert len(active) == 1
    assert active[0]["amount_cents"] == 9000


def test_cancel_within_48h_no_credit(client: TestClient, admin_headers, cliente_headers):
    slot_id = _open_slot(client, admin_headers, days=1)  # 24h away
    appt = _book(client, cliente_headers, slot_id)
    resp = client.delete(f"{APPTS}/{appt['id']}", headers=cliente_headers)
    assert resp.status_code == 200
    assert resp.json()["credit"] is None
    assert client.get(CREDITS, headers=cliente_headers).json() == []


def test_cancel_frees_the_slot(client: TestClient, admin_headers, cliente_headers):
    slot_id = _open_slot(client, admin_headers, days=6)
    appt = _book(client, cliente_headers, slot_id)
    # Booked → not in availability.
    assert all(
        s["id"] != slot_id
        for s in client.get(f"{AVAIL}{_range_query()}", headers=cliente_headers).json()
    )
    client.delete(f"{APPTS}/{appt['id']}", headers=cliente_headers)
    # Cancelled → the slot is bookable again.
    freed = client.get(f"{AVAIL}{_range_query()}", headers=cliente_headers).json()
    assert any(s["id"] == slot_id for s in freed)


def test_cancel_already_cancelled_returns_409(client: TestClient, admin_headers, cliente_headers):
    slot_id = _open_slot(client, admin_headers, days=5)
    appt = _book(client, cliente_headers, slot_id)
    client.delete(f"{APPTS}/{appt['id']}", headers=cliente_headers)
    again = client.delete(f"{APPTS}/{appt['id']}", headers=cliente_headers)
    assert again.status_code == 409


def test_cancel_others_appointment_returns_404(
    client: TestClient, admin_headers, cliente_headers, make_user
):
    slot_id = _open_slot(client, admin_headers, days=5)
    appt = _book(client, cliente_headers, slot_id)
    intruder = make_user("intruder@example.com", UserRole.cliente)
    resp = client.delete(f"{APPTS}/{appt['id']}", headers=_auth_header(intruder))
    assert resp.status_code == 404


# ------------------------------- reschedule ------------------------------- #


def test_reschedule_moves_to_new_slot(client: TestClient, admin_headers, cliente_headers):
    slot_a = _open_slot(client, admin_headers, days=5)
    slot_b = _open_slot(client, admin_headers, days=6)
    appt = _book(client, cliente_headers, slot_a)
    resp = client.patch(f"{APPTS}/{appt['id']}", json={"slot_id": slot_b}, headers=cliente_headers)
    assert resp.status_code == 200
    assert resp.json()["slot_id"] == slot_b
    avail = client.get(f"{AVAIL}{_range_query()}", headers=cliente_headers).json()
    ids = {s["id"] for s in avail}
    assert slot_a in ids  # old slot freed
    assert slot_b not in ids  # new slot held


def test_reschedule_to_taken_slot_returns_409(client: TestClient, admin_headers, cliente_headers):
    slot_a = _open_slot(client, admin_headers, days=5)
    slot_b = _open_slot(client, admin_headers, days=6)
    appt = _book(client, cliente_headers, slot_a)
    _book(client, cliente_headers, slot_b, visit_type="controllo")  # b is now held
    resp = client.patch(f"{APPTS}/{appt['id']}", json={"slot_id": slot_b}, headers=cliente_headers)
    assert resp.status_code == 409


def test_reschedule_cancelled_appointment_returns_409(
    client: TestClient, admin_headers, cliente_headers
):
    slot_a = _open_slot(client, admin_headers, days=5)
    slot_b = _open_slot(client, admin_headers, days=6)
    appt = _book(client, cliente_headers, slot_a)
    client.delete(f"{APPTS}/{appt['id']}", headers=cliente_headers)
    resp = client.patch(f"{APPTS}/{appt['id']}", json={"slot_id": slot_b}, headers=cliente_headers)
    assert resp.status_code == 409


def test_reschedule_unknown_slot_returns_404(client: TestClient, admin_headers, cliente_headers):
    slot_a = _open_slot(client, admin_headers, days=5)
    appt = _book(client, cliente_headers, slot_a)
    resp = client.patch(f"{APPTS}/{appt['id']}", json={"slot_id": 9999}, headers=cliente_headers)
    assert resp.status_code == 404


def test_reschedule_others_appointment_returns_404(
    client: TestClient, admin_headers, cliente_headers, make_user
):
    slot_a = _open_slot(client, admin_headers, days=5)
    slot_b = _open_slot(client, admin_headers, days=6)
    appt = _book(client, cliente_headers, slot_a)
    intruder = make_user("intruder2@example.com", UserRole.cliente)
    resp = client.patch(
        f"{APPTS}/{appt['id']}", json={"slot_id": slot_b}, headers=_auth_header(intruder)
    )
    assert resp.status_code == 404


# -------------------------------- credits --------------------------------- #


def test_credits_requires_auth(client: TestClient):
    assert client.get(CREDITS).status_code == 401


def test_credits_isolation_between_clients(
    client: TestClient, admin_headers, cliente_headers, make_user
):
    slot_id = _open_slot(client, admin_headers, days=5)
    appt = _book(client, cliente_headers, slot_id)
    client.delete(f"{APPTS}/{appt['id']}", headers=cliente_headers)  # cliente earns a credit
    other = make_user("other-credit@example.com", UserRole.cliente)
    assert client.get(CREDITS, headers=_auth_header(other)).json() == []


def test_expired_credit_is_excluded(client: TestClient, session: Session, make_user):
    cliente = make_user("expired@example.com", UserRole.cliente)
    session.add(
        Credit(
            client_id=cliente.id,
            amount_cents=9000,
            reason="Annullamento appuntamento",
            expires_at=datetime.now(UTC).replace(tzinfo=None) - timedelta(days=1),
        )
    )
    session.commit()
    assert client.get(CREDITS, headers=_auth_header(cliente)).json() == []
