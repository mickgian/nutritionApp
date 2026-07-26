"""Notification business logic: generate (idempotently) and list a client's own.

On read, the client's state is gathered and the rules produce the notifications
that apply; new ones are persisted (deduped by ``dedupe_key``) and the full list
is returned, newest first. This lazy-generate-on-read pattern needs no scheduler
(push delivery is a later integration). Ownership is enforced by the caller
passing ``client_id`` from the JWT.
"""

from datetime import UTC, datetime

from app.core.timeutils import to_utc
from app.models.appointment import Appointment, AppointmentStatus
from app.models.notification import Notification
from app.models.order import OrderStatus
from app.repositories.appointment_repository import AppointmentRepository
from app.repositories.notification_repository import NotificationRepository
from app.repositories.nutrition_plan_repository import NutritionPlanRepository
from app.repositories.order_repository import OrderRepository
from app.services.notification_rules import ClientState, NotificationCandidate, build_candidates


class NotificationService:
    def __init__(
        self,
        notifications: NotificationRepository,
        appointments: AppointmentRepository,
        plans: NutritionPlanRepository,
        orders: OrderRepository,
    ) -> None:
        self.notifications = notifications
        self.appointments = appointments
        self.plans = plans
        self.orders = orders

    def list_for(self, client_id: int, now: datetime | None = None) -> list[Notification]:
        moment = now or datetime.now(UTC)
        state = self._gather_state(client_id, moment)
        candidates = build_candidates(state, moment)
        self._persist_new(client_id, candidates)
        return self.notifications.list_by_client(client_id)

    def _gather_state(self, client_id: int, now: datetime) -> ClientState:
        appointments = self.appointments.list_by_client(client_id)
        active = [a for a in appointments if a.status != AppointmentStatus.cancelled]
        upcoming = self._next_upcoming(active, now)
        orders = self.orders.list_by_client(client_id)
        active_order = next((o for o in orders if o.status != OrderStatus.cancelled), None)
        return ClientState(
            upcoming_appointment=upcoming,
            has_appointment=len(active) > 0,
            active_plan=self.plans.get_active_by_client(client_id),
            active_order=active_order,
        )

    @staticmethod
    def _next_upcoming(appointments: list[Appointment], now: datetime) -> Appointment | None:
        future = [a for a in appointments if to_utc(a.scheduled_at) > now]
        return min(future, key=lambda a: to_utc(a.scheduled_at), default=None)

    def _persist_new(self, client_id: int, candidates: list[NotificationCandidate]) -> None:
        existing = self.notifications.existing_keys(client_id)
        fresh = [
            Notification(
                client_id=client_id,
                kind=candidate.kind,
                title=candidate.title,
                body=candidate.body,
                icon=candidate.icon,
                dedupe_key=candidate.dedupe_key,
            )
            for candidate in candidates
            if candidate.dedupe_key not in existing
        ]
        self.notifications.create_missing(fresh)
