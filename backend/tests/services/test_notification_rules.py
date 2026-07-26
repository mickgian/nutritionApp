"""Unit tests for the pure notification rules (DEV-060).

The rules are pure functions of (client state, now), so they're tested here
directly — no DB, no time mocking beyond passing an explicit ``now``.
"""

from datetime import UTC, datetime, timedelta

from app.models.appointment import Appointment, AppointmentStatus, VisitType
from app.models.notification import NotificationKind
from app.models.nutrition_plan import NutritionPlan
from app.models.order import Order, OrderFormula, OrderStatus, PickupSlot
from app.services.notification_rules import ClientState, build_candidates

NOW = datetime(2026, 7, 26, 12, 0, tzinfo=UTC)


def _appointment(hours_from_now: float) -> Appointment:
    return Appointment(
        id=1,
        client_id=1,
        slot_id=1,
        visit_type=VisitType.prima,
        scheduled_at=NOW + timedelta(hours=hours_from_now),
        status=AppointmentStatus.pending_payment,
        price_cents=9000,
    )


def _plan() -> NutritionPlan:
    return NutritionPlan(id=5, client_id=1, name="Dimagrimento 1400", daily_kcal=1400, weeks=4)


def _order() -> Order:
    return Order(
        id=8,
        client_id=1,
        plan_id=5,
        formula=OrderFormula.subscription,
        pickup=PickupSlot.mattina,
        status=OrderStatus.pending_payment,
        price_cents=7900,
    )


def _kinds(candidates) -> set[NotificationKind]:
    return {c.kind for c in candidates}


def test_no_state_yields_nothing():
    state = ClientState(
        upcoming_appointment=None, has_appointment=False, active_plan=None, active_order=None
    )
    assert build_candidates(state, NOW) == []


def test_appointment_within_24h_triggers_reminder():
    state = ClientState(
        upcoming_appointment=_appointment(20),
        has_appointment=True,
        active_plan=None,
        active_order=None,
    )
    kinds = _kinds(build_candidates(state, NOW))
    assert NotificationKind.appointment_reminder in kinds


def test_appointment_beyond_24h_no_reminder():
    state = ClientState(
        upcoming_appointment=_appointment(30),
        has_appointment=True,
        active_plan=None,
        active_order=None,
    )
    kinds = _kinds(build_candidates(state, NOW))
    assert NotificationKind.appointment_reminder not in kinds


def test_past_appointment_no_reminder():
    state = ClientState(
        upcoming_appointment=_appointment(-2),
        has_appointment=True,
        active_plan=None,
        active_order=None,
    )
    kinds = _kinds(build_candidates(state, NOW))
    assert NotificationKind.appointment_reminder not in kinds


def test_plan_without_order_triggers_order_deadline():
    state = ClientState(
        upcoming_appointment=None, has_appointment=True, active_plan=_plan(), active_order=None
    )
    candidates = build_candidates(state, NOW)
    assert NotificationKind.order_deadline in _kinds(candidates)
    # Not proposing the box: the client already has a plan.
    assert NotificationKind.box_proposal not in _kinds(candidates)


def test_active_order_triggers_box_ready_not_deadline():
    state = ClientState(
        upcoming_appointment=None, has_appointment=True, active_plan=_plan(), active_order=_order()
    )
    kinds = _kinds(build_candidates(state, NOW))
    assert NotificationKind.box_ready in kinds
    assert NotificationKind.order_deadline not in kinds


def test_consult_without_plan_proposes_box():
    state = ClientState(
        upcoming_appointment=None, has_appointment=True, active_plan=None, active_order=None
    )
    kinds = _kinds(build_candidates(state, NOW))
    assert NotificationKind.box_proposal in kinds


def test_dedupe_keys_are_stable_and_event_scoped():
    state = ClientState(
        upcoming_appointment=_appointment(10),
        has_appointment=True,
        active_plan=_plan(),
        active_order=_order(),
    )
    keys = {c.dedupe_key for c in build_candidates(state, NOW)}
    assert "appointment_reminder:1" in keys
    assert "box_ready:8" in keys
