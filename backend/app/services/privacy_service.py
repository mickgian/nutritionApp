"""GDPR data-portability and erasure service (DEV-093).

ADR-003 — Data retention & right to erasure
-------------------------------------------
Meridia processes health data (nutrition plans) and behavioural data
(orders, notifications), so a client's Art. 15 (access/portability) and Art. 17
(erasure) rights are first-class features rather than manual ops.

* **Export** (`GET /me/export`) returns everything we hold about the caller in a
  single portable JSON document. It excludes secrets that are ours, not the
  user's: password hashes and payment ``provider_token`` references never leave
  the server.

* **Erasure** (`DELETE /me`) hard-deletes the caller's health and behavioural
  records — nutrition plans, meal boxes, orders, payments, appointments,
  credits, notifications and reviews — and **anonymises** the account row rather
  than dropping it. Anonymisation (email → non-routable tombstone, name cleared,
  password made unusable, ``is_active`` → False) removes all personal identifiers
  while preserving referential integrity for any row that references
  ``users.id`` (e.g. ``assigned_by``). A tombstoned account cannot authenticate.

  Deletion happens in foreign-key-safe order (children before parents) and
  releases any availability slot the client's appointments were holding, so the
  studio's calendar frees up. Meridia issues no fiscal invoices in-app (payments
  are opaque provider tokens), so no statutory accounting-retention exception is
  exercised; revisit this ADR if in-app invoicing is added.
"""

import secrets
from datetime import UTC, datetime

import structlog
from sqlmodel import Session, select

from app.core.security import hash_password
from app.models.appointment import Appointment
from app.models.availability import AvailabilitySlot
from app.models.credit import Credit
from app.models.meal_box import BoxItem, MealBox
from app.models.notification import Notification
from app.models.nutrition_plan import NutritionPlan
from app.models.order import Order
from app.models.payment import Payment
from app.models.review import Review
from app.models.user import User
from app.schemas.privacy import (
    AccountExport,
    AppointmentExport,
    BoxExport,
    CreditExport,
    NotificationExport,
    OrderExport,
    PaymentExport,
    PlanExport,
    ReviewExport,
    UserDataExport,
)

logger = structlog.get_logger(__name__)


class PrivacyService:
    def __init__(self, session: Session) -> None:
        self.session = session

    def _owned(self, model, field: str = "client_id", value: int | None = None):
        return self.session.exec(select(model).where(getattr(model, field) == value)).all()

    def export(self, user: User) -> UserDataExport:
        """Aggregate every row the caller owns into one portable document."""
        uid = user.id
        return UserDataExport(
            generated_at=datetime.now(UTC),
            account=AccountExport(
                id=uid,
                email=user.email,
                full_name=user.full_name,
                role=user.role.value,
                is_active=user.is_active,
                created_at=user.created_at,
            ),
            appointments=[
                AppointmentExport(
                    id=a.id,
                    visit_type=a.visit_type.value,
                    scheduled_at=a.scheduled_at,
                    status=a.status.value,
                    price_cents=a.price_cents,
                    created_at=a.created_at,
                )
                for a in self._owned(Appointment, value=uid)
            ],
            orders=[
                OrderExport(
                    id=o.id,
                    plan_id=o.plan_id,
                    formula=o.formula.value,
                    pickup=o.pickup.value,
                    status=o.status.value,
                    price_cents=o.price_cents,
                    created_at=o.created_at,
                )
                for o in self._owned(Order, value=uid)
            ],
            payments=[
                PaymentExport(
                    id=p.id,
                    appointment_id=p.appointment_id,
                    order_id=p.order_id,
                    amount_cents=p.amount_cents,
                    method=p.method.value,
                    provider=p.provider,
                    status=p.status.value,
                    created_at=p.created_at,
                )
                for p in self._owned(Payment, value=uid)
            ],
            nutrition_plans=[
                PlanExport(
                    id=pl.id,
                    name=pl.name,
                    daily_kcal=pl.daily_kcal,
                    weeks=pl.weeks,
                    is_active=pl.is_active,
                    created_at=pl.created_at,
                )
                for pl in self._owned(NutritionPlan, value=uid)
            ],
            meal_boxes=[
                BoxExport(
                    id=b.id, plan_id=b.plan_id, week_index=b.week_index, created_at=b.created_at
                )
                for b in self._owned(MealBox, value=uid)
            ],
            credits=[
                CreditExport(
                    id=c.id,
                    amount_cents=c.amount_cents,
                    reason=c.reason,
                    is_used=c.is_used,
                    created_at=c.created_at,
                    expires_at=c.expires_at,
                )
                for c in self._owned(Credit, value=uid)
            ],
            notifications=[
                NotificationExport(
                    id=n.id,
                    kind=n.kind.value,
                    title=n.title,
                    body=n.body,
                    is_read=n.is_read,
                    created_at=n.created_at,
                )
                for n in self._owned(Notification, value=uid)
            ],
            reviews=[
                ReviewExport(
                    id=r.id,
                    professional_id=r.professional_id,
                    rating=r.rating,
                    body=r.body,
                    created_at=r.created_at,
                )
                for r in self._owned(Review, "author_id", uid)
            ],
        )

    def delete_account(self, user: User) -> None:
        """Erase the caller's data and anonymise the account (FK-safe order)."""
        uid = user.id
        appointment_ids = [a.id for a in self._owned(Appointment, value=uid)]

        for payment in self._owned(Payment, value=uid):
            self.session.delete(payment)
        for credit in self._owned(Credit, value=uid):
            self.session.delete(credit)
        for box in self._owned(MealBox, value=uid):
            for item in self.session.exec(select(BoxItem).where(BoxItem.box_id == box.id)).all():
                self.session.delete(item)
            self.session.delete(box)
        for order in self._owned(Order, value=uid):
            self.session.delete(order)

        self._release_slots(appointment_ids)
        for appointment in self._owned(Appointment, value=uid):
            self.session.delete(appointment)
        for plan in self._owned(NutritionPlan, value=uid):
            self.session.delete(plan)
        for notification in self._owned(Notification, value=uid):
            self.session.delete(notification)
        for review in self._owned(Review, "author_id", uid):
            self.session.delete(review)

        user.email = f"deleted-user-{uid}@meridia.invalid"
        user.full_name = "Utente eliminato"
        user.hashed_password = hash_password(secrets.token_urlsafe(32))
        user.is_active = False
        self.session.add(user)
        self.session.commit()

        logger.info(
            "account_erased",
            operation="gdpr_delete_account",
            user_id=uid,
            appointments_released=len(appointment_ids),
        )

    def _release_slots(self, appointment_ids: list[int]) -> None:
        if not appointment_ids:
            return
        held = self.session.exec(
            select(AvailabilitySlot).where(
                AvailabilitySlot.held_by_appointment_id.in_(appointment_ids)
            )
        ).all()
        for slot in held:
            slot.held_by_appointment_id = None
            slot.is_open = True
            self.session.add(slot)
