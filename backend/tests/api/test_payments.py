"""Tests for payment confirmation (DEV-070).

``POST /api/v1/payments`` charges a mock provider for an appointment or a box
order, records a Payment (never a PAN — only a provider reference), and confirms
the target: appointment → confirmed, order → paid. Amounts are server-computed
from the entity. Covers happy paths, server-authoritative amounts, idempotency,
the failure path (402), cancelled targets (409), ownership isolation, and auth.
"""

from datetime import UTC, datetime, timedelta

from fastapi.testclient import TestClient
from sqlmodel import Session

from app.core.security import create_access_token
from app.models.appointment import Appointment, AppointmentStatus, VisitType
from app.models.nutrition_plan import NutritionPlan
from app.models.order import Order, OrderFormula, OrderStatus, PickupSlot
from app.models.user import User, UserRole

PAYMENTS = "/api/v1/payments"


def _auth_header(user: User) -> dict[str, str]:
    return {"Authorization": f"Bearer {create_access_token(str(user.id), user.role.value)}"}


def _appointment(session: Session, client_id: int) -> Appointment:
    appt = Appointment(
        client_id=client_id,
        slot_id=None,
        visit_type=VisitType.prima,
        scheduled_at=datetime.now(UTC) + timedelta(days=5),
        status=AppointmentStatus.pending_payment,
        price_cents=9000,
    )
    session.add(appt)
    session.commit()
    session.refresh(appt)
    return appt


def _order(session: Session, client_id: int) -> Order:
    session.add(NutritionPlan(client_id=client_id, name="Piano", daily_kcal=1400, weeks=4))
    session.commit()
    order = Order(
        client_id=client_id,
        plan_id=1,
        formula=OrderFormula.subscription,
        pickup=PickupSlot.mattina,
        status=OrderStatus.pending_payment,
        price_cents=7900,
    )
    session.add(order)
    session.commit()
    session.refresh(order)
    return order


def _body(target: str, target_id: int, method: str = "card", token: str = "tok_ok") -> dict:
    return {"target": target, "target_id": target_id, "method": method, "token": token}


def test_requires_authentication(client: TestClient):
    assert client.post(PAYMENTS, json=_body("appointment", 1)).status_code == 401


def test_pay_appointment_confirms_it(client: TestClient, session: Session, make_user):
    cliente = make_user("payappt@example.com", UserRole.cliente)
    appt = _appointment(session, cliente.id)
    resp = client.post(PAYMENTS, json=_body("appointment", appt.id), headers=_auth_header(cliente))
    assert resp.status_code == 201
    body = resp.json()
    assert body["status"] == "succeeded"
    assert body["amount_cents"] == 9000  # server-authoritative, from the appointment
    # The appointment is now confirmed.
    listed = client.get("/api/v1/appointments", headers=_auth_header(cliente)).json()
    assert listed[0]["status"] == "confirmed"


def test_pay_order_marks_it_paid(client: TestClient, session: Session, make_user):
    cliente = make_user("payorder@example.com", UserRole.cliente)
    order = _order(session, cliente.id)
    resp = client.post(PAYMENTS, json=_body("order", order.id), headers=_auth_header(cliente))
    assert resp.status_code == 201
    assert resp.json()["amount_cents"] == 7900
    listed = client.get("/api/v1/orders", headers=_auth_header(cliente)).json()
    assert listed[0]["status"] == "paid"


def test_payment_never_stores_a_pan(client: TestClient, session: Session, make_user):
    cliente = make_user("nopan@example.com", UserRole.cliente)
    appt = _appointment(session, cliente.id)
    resp = client.post(
        PAYMENTS,
        json=_body("appointment", appt.id, token="4111111111111111"),
        headers=_auth_header(cliente),
    )
    assert resp.status_code == 201
    # The stored provider_token is a reference, not the raw token we sent.
    assert "provider_token" in resp.json()
    assert resp.json()["provider_token"] != "4111111111111111"


def test_pay_is_idempotent(client: TestClient, session: Session, make_user):
    cliente = make_user("idempay@example.com", UserRole.cliente)
    appt = _appointment(session, cliente.id)
    first = client.post(PAYMENTS, json=_body("appointment", appt.id), headers=_auth_header(cliente))
    second = client.post(
        PAYMENTS, json=_body("appointment", appt.id), headers=_auth_header(cliente)
    )
    assert first.status_code == 201
    assert second.status_code == 200  # returns the existing payment, no re-charge
    assert first.json()["id"] == second.json()["id"]


def test_failed_charge_returns_402_and_does_not_confirm(
    client: TestClient, session: Session, make_user
):
    cliente = make_user("failpay@example.com", UserRole.cliente)
    appt = _appointment(session, cliente.id)
    resp = client.post(
        PAYMENTS,
        json=_body("appointment", appt.id, token="tok_fail"),
        headers=_auth_header(cliente),
    )
    assert resp.status_code == 402
    # The appointment stays pending_payment.
    listed = client.get("/api/v1/appointments", headers=_auth_header(cliente)).json()
    assert listed[0]["status"] == "pending_payment"


def test_pay_cancelled_target_returns_409(client: TestClient, session: Session, make_user):
    cliente = make_user("cancelpay@example.com", UserRole.cliente)
    appt = _appointment(session, cliente.id)
    appt.status = AppointmentStatus.cancelled
    session.add(appt)
    session.commit()
    resp = client.post(PAYMENTS, json=_body("appointment", appt.id), headers=_auth_header(cliente))
    assert resp.status_code == 409


def test_pay_order_is_idempotent(client: TestClient, session: Session, make_user):
    cliente = make_user("idemorder@example.com", UserRole.cliente)
    order = _order(session, cliente.id)
    first = client.post(PAYMENTS, json=_body("order", order.id), headers=_auth_header(cliente))
    second = client.post(PAYMENTS, json=_body("order", order.id), headers=_auth_header(cliente))
    assert first.status_code == 201
    assert second.status_code == 200
    assert first.json()["id"] == second.json()["id"]


def test_pay_cancelled_order_returns_409(client: TestClient, session: Session, make_user):
    cliente = make_user("cancelorder@example.com", UserRole.cliente)
    order = _order(session, cliente.id)
    order.status = OrderStatus.cancelled
    session.add(order)
    session.commit()
    resp = client.post(PAYMENTS, json=_body("order", order.id), headers=_auth_header(cliente))
    assert resp.status_code == 409


def test_pay_unknown_target_returns_404(client: TestClient, cliente_headers):
    assert (
        client.post(PAYMENTS, json=_body("appointment", 9999), headers=cliente_headers).status_code
        == 404
    )
    assert (
        client.post(PAYMENTS, json=_body("order", 9999), headers=cliente_headers).status_code == 404
    )


def test_pay_someone_elses_target_returns_404(client: TestClient, session: Session, make_user):
    owner = make_user("owner@example.com", UserRole.cliente)
    appt = _appointment(session, owner.id)
    intruder = make_user("intruder@example.com", UserRole.cliente)
    resp = client.post(PAYMENTS, json=_body("appointment", appt.id), headers=_auth_header(intruder))
    assert resp.status_code == 404


def test_invalid_method_returns_422(client: TestClient, session: Session, make_user):
    cliente = make_user("badmethod@example.com", UserRole.cliente)
    appt = _appointment(session, cliente.id)
    resp = client.post(
        PAYMENTS,
        json=_body("appointment", appt.id, method="bitcoin"),
        headers=_auth_header(cliente),
    )
    assert resp.status_code == 422
