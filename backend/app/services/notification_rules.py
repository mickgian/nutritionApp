"""Behavior-profiled notification rules (DEV-060).

Pure functions: given a client's aggregated state and a reference ``now``, they
return the notifications that apply — no I/O, no persistence, so the rules are
deterministic and unit-testable. The service layer gathers the state, persists
new candidates idempotently (by ``dedupe_key``), and lists them.

Implemented rules (derivable from current domain state):
- appointment_reminder — an upcoming appointment within the next 24h.
- order_deadline — an active plan but no active box order yet.
- box_ready — an active box order (in preparation / to collect).
- box_proposal — has been to a consultation but has no active plan yet.

Rules that need data not yet modelled (birthday, inactivity win-back, the
8-box check-up, promos) are intentionally out of scope here.
"""

from dataclasses import dataclass
from datetime import datetime, timedelta

from app.core.timeutils import to_utc
from app.models.appointment import Appointment
from app.models.notification import NotificationKind
from app.models.nutrition_plan import NutritionPlan
from app.models.order import Order

APPOINTMENT_REMINDER_WINDOW_HOURS = 24


@dataclass(frozen=True)
class ClientState:
    """The slice of a client's state the notification rules read."""

    upcoming_appointment: Appointment | None
    has_appointment: bool
    active_plan: NutritionPlan | None
    active_order: Order | None


@dataclass(frozen=True)
class NotificationCandidate:
    kind: NotificationKind
    icon: str
    title: str
    body: str
    dedupe_key: str


def build_candidates(state: ClientState, now: datetime) -> list[NotificationCandidate]:
    candidates: list[NotificationCandidate] = []

    appointment = state.upcoming_appointment
    if appointment is not None:
        delta = to_utc(appointment.scheduled_at) - now
        if timedelta() < delta <= timedelta(hours=APPOINTMENT_REMINDER_WINDOW_HOURS):
            candidates.append(
                NotificationCandidate(
                    kind=NotificationKind.appointment_reminder,
                    icon="⏰",
                    title="Promemoria appuntamento",
                    body=(
                        "Domani hai la tua visita in studio, in Via dei Gelsomini 12 a "
                        "Pachino. Ti aspettiamo!"
                    ),
                    dedupe_key=f"appointment_reminder:{appointment.id}",
                )
            )

    if state.active_plan is not None and state.active_order is None:
        candidates.append(
            NotificationCandidate(
                kind=NotificationKind.order_deadline,
                icon="🍱",
                title="Ordina il box entro giovedì",
                body=(
                    "La settimana sta finendo: conferma il box di lunedì entro giovedì alle 20:00."
                ),
                dedupe_key=f"order_deadline:{state.active_plan.id}",
            )
        )

    if state.active_order is not None:
        candidates.append(
            NotificationCandidate(
                kind=NotificationKind.box_ready,
                icon="📦",
                title="Il tuo box è pronto!",
                body=(
                    "Ti aspetta in studio (Via dei Gelsomini 12) per il ritiro nella "
                    "fascia che hai scelto."
                ),
                dedupe_key=f"box_ready:{state.active_order.id}",
            )
        )

    if state.has_appointment and state.active_plan is None:
        candidates.append(
            NotificationCandidate(
                kind=NotificationKind.box_proposal,
                icon="🥗",
                title="La dieta senza cucinare",
                body=(
                    "Hai fatto la consulenza: il tuo piano è disponibile anche come box "
                    "di pasti pronti. Dai un'occhiata."
                ),
                dedupe_key="box_proposal",
            )
        )

    return candidates
