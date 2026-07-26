"""GDPR endpoints (DEV-093): personal-data export and account erasure.

Covers the caller's right to data portability (GET /me/export) and erasure
(DELETE /me), per-user isolation, the anonymize-and-release semantics of a
deletion, and that a deleted account can no longer authenticate.
"""

from datetime import UTC, datetime, timedelta

from fastapi.testclient import TestClient
from sqlmodel import Session, select

from app.core.security import create_access_token
from app.models.appointment import Appointment, VisitType
from app.models.availability import AvailabilitySlot
from app.models.credit import Credit
from app.models.meal import Meal, MealSlot
from app.models.meal_box import BoxItem, MealBox
from app.models.notification import Notification, NotificationKind
from app.models.nutrition_plan import NutritionPlan
from app.models.order import Order, OrderFormula, PickupSlot
from app.models.payment import Payment, PaymentMethod, PaymentStatus
from app.models.review import Review
from app.models.user import User


def _headers(user: User) -> dict[str, str]:
    token = create_access_token(subject=str(user.id), role=user.role.value)
    return {"Authorization": f"Bearer {token}"}


def _seed_full_client(session: Session, uid: int, tag: str = "a") -> int:
    """Seed one row in every table a client owns. Returns the held slot id."""
    plan = NutritionPlan(client_id=uid, name="Dimagrimento 1400 kcal", daily_kcal=1400, weeks=4)
    session.add(plan)
    session.commit()
    session.refresh(plan)

    order = Order(
        client_id=uid,
        plan_id=plan.id,
        formula=OrderFormula.single,
        pickup=PickupSlot.mattina,
        price_cents=8900,
    )
    session.add(order)
    session.commit()
    session.refresh(order)

    appt = Appointment(
        client_id=uid,
        visit_type=VisitType.prima,
        scheduled_at=datetime.now(UTC) + timedelta(days=3),
        price_cents=9000,
    )
    session.add(appt)
    session.commit()
    session.refresh(appt)

    slot = AvailabilitySlot(
        starts_at=datetime.now(UTC) + timedelta(days=3, hours=int(uid)),
        is_open=False,
        held_by_appointment_id=appt.id,
    )
    session.add(slot)
    session.commit()
    session.refresh(slot)

    meal = Meal(title="Insalata", slot=MealSlot.lunch, kcal=400, protein_g=30, carb_g=20, fat_g=10)
    session.add(meal)
    session.commit()
    session.refresh(meal)

    box = MealBox(client_id=uid, plan_id=plan.id, week_index=0)
    session.add(box)
    session.commit()
    session.refresh(box)

    prof_review = _seed_review(session, uid, tag)

    session.add(
        Payment(
            client_id=uid,
            order_id=order.id,
            amount_cents=8900,
            method=PaymentMethod.card,
            provider="mock",
            provider_token="tok_secret_should_never_export",
            status=PaymentStatus.succeeded,
        )
    )
    session.add(
        Credit(
            client_id=uid,
            amount_cents=5000,
            reason="Annullamento appuntamento",
            expires_at=datetime.now(UTC) + timedelta(days=180),
        )
    )
    session.add(
        Notification(
            client_id=uid,
            kind=NotificationKind.box_ready,
            title="Box pronto",
            body="Il tuo box è pronto per il ritiro.",
            icon="📦",
            dedupe_key=f"box:{tag}:{uid}",
        )
    )
    session.add(BoxItem(box_id=box.id, meal_id=meal.id, weekday=0, slot=MealSlot.lunch))
    session.add(prof_review)
    session.commit()
    return slot.id


def _seed_review(session: Session, uid: int, tag: str) -> Review:
    from app.models.professional import Professional

    prof = Professional(full_name="Dott.ssa Meridia", title="Nutrizionista", bio="Bio")
    session.add(prof)
    session.commit()
    session.refresh(prof)
    return Review(
        professional_id=prof.id,
        author_id=uid,
        author_name=f"Cliente {tag}",
        rating=5,
        body="Ottima esperienza.",
    )


def _owned_counts(session: Session, uid: int) -> dict[str, int]:
    def n(model, field="client_id") -> int:
        return len(session.exec(select(model).where(getattr(model, field) == uid)).all())

    return {
        "appointments": n(Appointment),
        "orders": n(Order),
        "payments": n(Payment),
        "plans": n(NutritionPlan),
        "boxes": n(MealBox),
        "credits": n(Credit),
        "notifications": n(Notification),
        "reviews": n(Review, "author_id"),
    }


# --------------------------------------------------------------------------- #
# Export                                                                       #
# --------------------------------------------------------------------------- #


def test_export_requires_auth(client: TestClient):
    assert client.get("/api/v1/me/export").status_code == 401


def test_export_returns_all_owned_data(client: TestClient, session: Session, make_user):
    user = make_user("mario@example.com")
    _seed_full_client(session, user.id)

    res = client.get("/api/v1/me/export", headers=_headers(user))
    assert res.status_code == 200
    body = res.json()

    assert body["account"]["email"] == "mario@example.com"
    assert body["account"]["full_name"] == "Test User"
    for section in (
        "appointments",
        "orders",
        "payments",
        "nutrition_plans",
        "meal_boxes",
        "credits",
        "notifications",
        "reviews",
    ):
        assert len(body[section]) >= 1, f"export missing {section}"


def test_export_never_leaks_payment_provider_token(client: TestClient, session: Session, make_user):
    user = make_user("mario@example.com")
    _seed_full_client(session, user.id)

    res = client.get("/api/v1/me/export", headers=_headers(user))
    assert res.status_code == 200
    assert "provider_token" not in res.json()["payments"][0]
    assert "tok_secret_should_never_export" not in res.text


def test_export_is_isolated_per_user(client: TestClient, session: Session, make_user):
    mario = make_user("mario@example.com")
    lucia = make_user("lucia@example.com")
    _seed_full_client(session, mario.id, tag="mario")
    _seed_full_client(session, lucia.id, tag="lucia")

    body = client.get("/api/v1/me/export", headers=_headers(mario)).json()
    plan_owners = {p["name"] for p in body["nutrition_plans"]}
    assert body["account"]["email"] == "mario@example.com"
    # Only one plan (mario's) — lucia's rows must not appear.
    assert len(body["nutrition_plans"]) == 1
    assert plan_owners == {"Dimagrimento 1400 kcal"}
    assert len(body["notifications"]) == 1


# --------------------------------------------------------------------------- #
# Deletion                                                                     #
# --------------------------------------------------------------------------- #


def test_delete_requires_auth(client: TestClient):
    assert client.delete("/api/v1/me").status_code == 401


def test_delete_erases_owned_data_and_anonymizes_account(
    client: TestClient, session: Session, make_user
):
    user = make_user("mario@example.com")
    slot_id = _seed_full_client(session, user.id)
    assert all(v >= 1 for v in _owned_counts(session, user.id).values())

    res = client.delete("/api/v1/me", headers=_headers(user))
    assert res.status_code == 204

    session.expire_all()
    assert _owned_counts(session, user.id) == {
        "appointments": 0,
        "orders": 0,
        "payments": 0,
        "plans": 0,
        "boxes": 0,
        "credits": 0,
        "notifications": 0,
        "reviews": 0,
    }
    # Box items go with their boxes.
    assert session.exec(select(BoxItem)).all() == []

    refreshed = session.get(User, user.id)
    assert refreshed is not None  # row kept (anonymized), FK integrity preserved
    assert refreshed.is_active is False
    assert refreshed.email != "mario@example.com"
    assert refreshed.full_name == "Utente eliminato"

    # The availability slot the appointment held is released back to the studio.
    slot = session.get(AvailabilitySlot, slot_id)
    assert slot.held_by_appointment_id is None
    assert slot.is_open is True


def test_delete_blocks_subsequent_authentication(client: TestClient, session: Session, make_user):
    user = make_user("mario@example.com")
    headers = _headers(user)
    client.delete("/api/v1/me", headers=headers)

    # Same (still-unexpired) token no longer resolves — account is inactive.
    assert client.get("/api/v1/me/export", headers=headers).status_code == 401
    # And the old credentials cannot log in.
    login = client.post(
        "/api/v1/auth/login",
        json={"email": "mario@example.com", "password": "password123"},
    )
    assert login.status_code == 401


def test_delete_is_isolated_per_user(client: TestClient, session: Session, make_user):
    mario = make_user("mario@example.com")
    lucia = make_user("lucia@example.com")
    _seed_full_client(session, mario.id, tag="mario")
    _seed_full_client(session, lucia.id, tag="lucia")

    assert client.delete("/api/v1/me", headers=_headers(mario)).status_code == 204

    session.expire_all()
    # Lucia is untouched.
    assert all(v >= 1 for v in _owned_counts(session, lucia.id).values())
    assert session.get(User, lucia.id).is_active is True
