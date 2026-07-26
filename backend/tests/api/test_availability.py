"""Tests for the admin availability endpoints (DEV-020).

Studio (admin) opens/closes consultation slots; clients never reach these.
Covers role enforcement, upsert toggling, past-date rejection, range listing,
and the guard against closing a slot already held by a booking.
"""

from datetime import UTC, datetime, timedelta

from fastapi.testclient import TestClient
from sqlmodel import Session, select

from app.models.appointment import Appointment, VisitType
from app.models.availability import AvailabilitySlot

BASE = "/api/v1/admin/availability"


def _future(days: int = 3, hour: int = 10) -> str:
    dt = datetime.now(UTC) + timedelta(days=days)
    return dt.replace(hour=hour, minute=0, second=0, microsecond=0).isoformat()


# ------------------------------ role guard -------------------------------- #


def test_put_requires_authentication(client: TestClient):
    resp = client.put(BASE, json={"starts_at": _future(), "is_open": True})
    assert resp.status_code == 401


def test_put_forbidden_for_cliente(client: TestClient, cliente_headers):
    resp = client.put(BASE, json={"starts_at": _future(), "is_open": True}, headers=cliente_headers)
    assert resp.status_code == 403
    assert resp.json()["detail"] == "Accesso riservato allo studio"


def test_get_forbidden_for_cliente(client: TestClient, cliente_headers):
    today = datetime.now(UTC).date().isoformat()
    resp = client.get(f"{BASE}?from={today}&to={today}", headers=cliente_headers)
    assert resp.status_code == 403


# ------------------------------ open / close ------------------------------ #


def test_admin_opens_slot(client: TestClient, admin_headers):
    starts = _future()
    resp = client.put(BASE, json={"starts_at": starts, "is_open": True}, headers=admin_headers)
    assert resp.status_code == 200
    body = resp.json()
    assert body["is_open"] is True
    assert body["duration_min"] == 60
    assert "id" in body


def test_put_is_idempotent_upsert_not_duplicate(
    client: TestClient, admin_headers, session: Session
):
    starts = _future()
    first = client.put(BASE, json={"starts_at": starts, "is_open": True}, headers=admin_headers)
    second = client.put(BASE, json={"starts_at": starts, "is_open": False}, headers=admin_headers)
    assert first.status_code == 200
    assert second.status_code == 200
    assert first.json()["id"] == second.json()["id"]
    assert second.json()["is_open"] is False
    rows = session.exec(select(AvailabilitySlot)).all()
    assert len(rows) == 1


def test_open_slot_with_naive_datetime_assumed_utc(client: TestClient, admin_headers):
    naive = (datetime.now(UTC) + timedelta(days=4)).replace(tzinfo=None, microsecond=0).isoformat()
    resp = client.put(BASE, json={"starts_at": naive, "is_open": True}, headers=admin_headers)
    assert resp.status_code == 200
    assert resp.json()["is_open"] is True


def test_admin_can_set_custom_duration(client: TestClient, admin_headers):
    resp = client.put(
        BASE,
        json={"starts_at": _future(), "duration_min": 30, "is_open": True},
        headers=admin_headers,
    )
    assert resp.status_code == 200
    assert resp.json()["duration_min"] == 30


# ------------------------------ edge cases -------------------------------- #


def test_open_past_slot_rejected(client: TestClient, admin_headers):
    past = (datetime.now(UTC) - timedelta(days=1)).isoformat()
    resp = client.put(BASE, json={"starts_at": past, "is_open": True}, headers=admin_headers)
    assert resp.status_code == 400
    assert "passato" in resp.json()["detail"].lower()


def test_cannot_close_slot_held_by_appointment(
    client: TestClient, admin_headers, make_user, session: Session
):
    owner = make_user("owner@example.com")
    appt = Appointment(
        client_id=owner.id,
        visit_type=VisitType.prima,
        scheduled_at=datetime.now(UTC) + timedelta(days=3),
        price_cents=9000,
    )
    session.add(appt)
    session.commit()
    session.refresh(appt)

    starts = datetime.now(UTC) + timedelta(days=3)
    slot = AvailabilitySlot(starts_at=starts, is_open=True, held_by_appointment_id=appt.id)
    session.add(slot)
    session.commit()

    resp = client.put(
        BASE, json={"starts_at": starts.isoformat(), "is_open": False}, headers=admin_headers
    )
    assert resp.status_code == 409
    assert resp.json()["detail"] == "Orario già prenotato, impossibile chiuderlo"


# --------------------------------- list ----------------------------------- #


def test_list_returns_slots_in_range(client: TestClient, admin_headers):
    in_range = _future(days=2)
    out_of_range = _future(days=20)
    client.put(BASE, json={"starts_at": in_range, "is_open": True}, headers=admin_headers)
    client.put(BASE, json={"starts_at": out_of_range, "is_open": True}, headers=admin_headers)

    start = datetime.now(UTC).date().isoformat()
    end = (datetime.now(UTC) + timedelta(days=5)).date().isoformat()
    resp = client.get(f"{BASE}?from={start}&to={end}", headers=admin_headers)
    assert resp.status_code == 200
    items = resp.json()
    assert len(items) == 1
    assert items[0]["starts_at"].startswith(in_range[:10])
