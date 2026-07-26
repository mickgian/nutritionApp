"""Appointment lifecycle: reschedule and cancel (with the 48h credit rule).

A client may move or cancel only their own appointment (else 404 — a missing or
foreign row is indistinguishable to avoid leaking existence). Cancelling more
than 48h before the visit refunds the price as a 6-month store credit; within
48h the cancellation is allowed but earns no credit (the rule is enforced here,
server-side, not merely in the UI). Cancelling frees the held slot so it can be
booked again. Expected conditions raise 4xx (404/409); never 500.
"""

from datetime import UTC, datetime, timedelta

import structlog
from fastapi import HTTPException, status

from app.core.timeutils import to_utc
from app.models.appointment import Appointment, AppointmentStatus
from app.models.credit import CANCELLATION_CREDIT_WINDOW_HOURS, CREDIT_VALIDITY_DAYS, Credit
from app.repositories.appointment_repository import AppointmentRepository
from app.repositories.availability_repository import AvailabilityRepository
from app.repositories.credit_repository import CreditRepository
from app.schemas.appointment import AppointmentReschedule

logger = structlog.get_logger(__name__)


class AppointmentLifecycleService:
    def __init__(
        self,
        appointments: AppointmentRepository,
        slots: AvailabilityRepository,
        credit_repo: CreditRepository,
    ) -> None:
        self.appointments = appointments
        self.slots = slots
        self.credits = credit_repo

    def reschedule(
        self, client_id: int, appointment_id: int, data: AppointmentReschedule
    ) -> Appointment:
        appointment = self._owned(appointment_id, client_id)
        if appointment.status == AppointmentStatus.cancelled:
            raise HTTPException(
                status_code=status.HTTP_409_CONFLICT, detail="Appuntamento annullato"
            )
        new_slot = self.slots.get_by_id(data.slot_id)
        if new_slot is None:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Orario non trovato")
        if (
            not new_slot.is_open
            or new_slot.held_by_appointment_id is not None
            or to_utc(new_slot.starts_at) <= datetime.now(UTC)
        ):
            raise HTTPException(
                status_code=status.HTTP_409_CONFLICT, detail="Orario non più disponibile"
            )
        self._release_slot(appointment.slot_id)
        new_slot.held_by_appointment_id = appointment.id
        self.slots.save(new_slot)
        appointment.slot_id = new_slot.id
        appointment.scheduled_at = new_slot.starts_at
        saved = self.appointments.save(appointment)
        logger.info(
            "appointment_rescheduled",
            operation="reschedule",
            user_id=client_id,
            appointment_id=appointment.id,
            slot_id=new_slot.id,
        )
        return saved

    def cancel(self, client_id: int, appointment_id: int) -> tuple[Appointment, Credit | None]:
        appointment = self._owned(appointment_id, client_id)
        if appointment.status == AppointmentStatus.cancelled:
            raise HTTPException(
                status_code=status.HTTP_409_CONFLICT, detail="Appuntamento già annullato"
            )
        now = datetime.now(UTC)
        credit = None
        if to_utc(appointment.scheduled_at) - now > timedelta(
            hours=CANCELLATION_CREDIT_WINDOW_HOURS
        ):
            credit = self._issue_credit(appointment, now)
        appointment.status = AppointmentStatus.cancelled
        self._release_slot(appointment.slot_id)
        self.appointments.save(appointment)
        logger.info(
            "appointment_cancelled",
            operation="cancel",
            user_id=client_id,
            appointment_id=appointment.id,
            credit_issued=credit is not None,
        )
        return appointment, credit

    def _owned(self, appointment_id: int, client_id: int) -> Appointment:
        appointment = self.appointments.get_by_id(appointment_id)
        if appointment is None or appointment.client_id != client_id:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND, detail="Appuntamento non trovato"
            )
        return appointment

    def _release_slot(self, slot_id: int | None) -> None:
        if slot_id is None:
            return
        slot = self.slots.get_by_id(slot_id)
        if slot is not None:
            slot.held_by_appointment_id = None
            self.slots.save(slot)

    def _issue_credit(self, appointment: Appointment, now: datetime) -> Credit:
        credit = Credit(
            client_id=appointment.client_id,
            amount_cents=appointment.price_cents,
            reason="Annullamento appuntamento",
            source_appointment_id=appointment.id,
            expires_at=now + timedelta(days=CREDIT_VALIDITY_DAYS),
        )
        return self.credits.create(credit)
