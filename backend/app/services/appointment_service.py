"""Booking business logic: list open slots, book, list the caller's own.

Ownership is enforced by the caller passing ``client_id`` from the JWT; a client
never touches another's rows. Expected conditions raise 4xx (404/409); never 500.
"""

from datetime import UTC, date, datetime, time

import structlog
from fastapi import HTTPException, status

from app.core.timeutils import to_utc
from app.models.appointment import VISIT_PRICE_CENTS, Appointment
from app.models.availability import AvailabilitySlot
from app.repositories.appointment_repository import AppointmentRepository
from app.repositories.availability_repository import AvailabilityRepository
from app.schemas.appointment import AppointmentCreate

logger = structlog.get_logger(__name__)


class AppointmentService:
    def __init__(
        self,
        appointments: AppointmentRepository,
        slots: AvailabilityRepository,
    ) -> None:
        self.appointments = appointments
        self.slots = slots

    def list_open_slots(self, from_date: date, to_date: date) -> list[AvailabilitySlot]:
        start = datetime.combine(from_date, time.min, tzinfo=UTC)
        end = datetime.combine(to_date, time.max, tzinfo=UTC)
        now = datetime.now(UTC)
        return [
            slot
            for slot in self.slots.list_between(start, end)
            if slot.is_open and slot.held_by_appointment_id is None and to_utc(slot.starts_at) > now
        ]

    def book(self, client_id: int, data: AppointmentCreate) -> Appointment:
        slot = self.slots.get_by_id(data.slot_id)
        if slot is None:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Orario non trovato")
        if (
            not slot.is_open
            or slot.held_by_appointment_id is not None
            or to_utc(slot.starts_at) <= datetime.now(UTC)
        ):
            raise HTTPException(
                status_code=status.HTTP_409_CONFLICT, detail="Orario non più disponibile"
            )

        appointment = Appointment(
            client_id=client_id,
            slot_id=slot.id,
            visit_type=data.visit_type,
            scheduled_at=slot.starts_at,
            price_cents=VISIT_PRICE_CENTS[data.visit_type],
        )
        created = self.appointments.create(appointment)

        slot.held_by_appointment_id = created.id
        self.slots.save(slot)
        logger.info(
            "appointment_booked",
            operation="book",
            user_id=client_id,
            appointment_id=created.id,
            slot_id=slot.id,
        )
        return created

    def list_mine(self, client_id: int) -> list[Appointment]:
        return self.appointments.list_by_client(client_id)
