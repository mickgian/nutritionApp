"""Tests for the client booking flow (DEV-021).

A client lists open slots, books a prima/controllo on one, and sees only their
own appointments. Covers ownership isolation, the slot lifecycle (a booked slot
disappears from availability), and the 404/409/422 edges.
"""

from datetime import UTC, datetime, timedelta

from fastapi.testclient import TestClient
from sqlmodel import Session

from app.core.security import create_access_token
from app.models.availability import AvailabilitySlot

AVAIL = "/api/v1/availability"
APPTS = "/api/v1/appointments"
ADMIN_AVAIL = "/api/v1/admin/availability"


def _future_iso(days: int = 3, hour: int = 10) -> str:
    return (
        (datetime.now(UTC) + timedelta(days=days))
        .replace(hour=hour, minute=0, second=0, microsecond=0)
        .isoformat()
    )


def _open_slot(client: TestClient, admin_headers, starts_iso: str, duration: int = 60) -> int:
    resp = client.put(
        ADMIN_AVAIL,
        json={"starts_at": starts_iso, "duration_min": duration, "is_open": True},
        headers=admin_headers,
    )
    assert resp.status_code == 200
    return resp.json()["id"]


def _range_query() -> str:
    start = datetime.now(UTC).date().isoformat()
    end = (datetime.now(UTC) + timedelta(days=30)).date().isoformat()
    return f"?from={start}&to={end}"


# ----------------------------- availability ------------------------------- #


def test_availability_requires_auth(client: TestClient):
    assert client.get(f"{AVAIL}{_range_query()}").status_code == 401


def test_availability_lists_open_future_slots(client: TestClient, admin_headers, cliente_headers):
    _open_slot(client, admin_headers, _future_iso(days=2))
    resp = client.get(f"{AVAIL}{_range_query()}", headers=cliente_headers)
    assert resp.status_code == 200
    slots = resp.json()
    assert len(slots) == 1
    assert set(slots[0].keys()) == {"id", "starts_at", "duration_min"}


# ------------------------------- booking ---------------------------------- #


def test_book_creates_pending_appointment_with_server_price(
    client: TestClient, admin_headers, cliente_headers
):
    slot_id = _open_slot(client, admin_headers, _future_iso())
    resp = client.post(
        APPTS, json={"slot_id": slot_id, "visit_type": "prima"}, headers=cliente_headers
    )
    assert resp.status_code == 201
    body = resp.json()
    assert body["status"] == "pending_payment"
    assert body["visit_type"] == "prima"
    assert body["price_cents"] == 9000  # server-authoritative, ignores any client price
    assert body["slot_id"] == slot_id


def test_controllo_is_cheaper(client: TestClient, admin_headers, cliente_headers):
    slot_id = _open_slot(client, admin_headers, _future_iso(days=4))
    resp = client.post(
        APPTS, json={"slot_id": slot_id, "visit_type": "controllo"}, headers=cliente_headers
    )
    assert resp.status_code == 201
    assert resp.json()["price_cents"] == 5000


def test_booked_slot_disappears_from_availability(
    client: TestClient, admin_headers, cliente_headers
):
    slot_id = _open_slot(client, admin_headers, _future_iso(days=5))
    client.post(APPTS, json={"slot_id": slot_id, "visit_type": "prima"}, headers=cliente_headers)
    resp = client.get(f"{AVAIL}{_range_query()}", headers=cliente_headers)
    assert all(s["id"] != slot_id for s in resp.json())


def test_book_unknown_slot_returns_404(client: TestClient, cliente_headers):
    resp = client.post(
        APPTS, json={"slot_id": 9999, "visit_type": "prima"}, headers=cliente_headers
    )
    assert resp.status_code == 404
    assert resp.json()["detail"] == "Orario non trovato"


def test_book_taken_slot_returns_409(client: TestClient, admin_headers, cliente_headers):
    slot_id = _open_slot(client, admin_headers, _future_iso(days=6))
    first = client.post(
        APPTS, json={"slot_id": slot_id, "visit_type": "prima"}, headers=cliente_headers
    )
    second = client.post(
        APPTS, json={"slot_id": slot_id, "visit_type": "prima"}, headers=cliente_headers
    )
    assert first.status_code == 201
    assert second.status_code == 409
    assert second.json()["detail"] == "Orario non più disponibile"


def test_book_past_slot_returns_409(client: TestClient, cliente_headers, session: Session):
    past = AvailabilitySlot(
        starts_at=datetime.now(UTC).replace(tzinfo=None) - timedelta(days=1),
        is_open=True,
    )
    session.add(past)
    session.commit()
    session.refresh(past)
    resp = client.post(
        APPTS, json={"slot_id": past.id, "visit_type": "prima"}, headers=cliente_headers
    )
    assert resp.status_code == 409


def test_book_invalid_visit_type_returns_422(client: TestClient, admin_headers, cliente_headers):
    slot_id = _open_slot(client, admin_headers, _future_iso(days=7))
    resp = client.post(
        APPTS, json={"slot_id": slot_id, "visit_type": "massaggio"}, headers=cliente_headers
    )
    assert resp.status_code == 422


# ------------------------- list own (isolation) --------------------------- #


def test_list_appointments_requires_auth(client: TestClient):
    assert client.get(APPTS).status_code == 401


def test_client_sees_only_own_appointments(
    client: TestClient, admin_headers, cliente_headers, make_user
):
    slot_id = _open_slot(client, admin_headers, _future_iso(days=8))
    client.post(APPTS, json={"slot_id": slot_id, "visit_type": "prima"}, headers=cliente_headers)

    other = make_user("other@example.com")
    other_headers = {
        "Authorization": f"Bearer {create_access_token(str(other.id), other.role.value)}"
    }

    mine = client.get(APPTS, headers=cliente_headers).json()
    theirs = client.get(APPTS, headers=other_headers).json()
    assert len(mine) == 1
    assert theirs == []
