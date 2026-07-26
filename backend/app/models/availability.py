"""AvailabilitySlot model — studio-defined consultation openings.

The studio (admin) opens/closes time slots; clients book an appointment on an
open slot (DEV-021). Datetimes are timezone-aware UTC; ``starts_at`` is unique so
the same instant can't be opened twice.
"""

from datetime import UTC, datetime

from sqlmodel import Field, SQLModel


class AvailabilitySlot(SQLModel, table=True):
    __tablename__ = "availability_slots"

    id: int | None = Field(default=None, primary_key=True)
    starts_at: datetime = Field(index=True, unique=True)
    duration_min: int = Field(default=60)
    is_open: bool = Field(default=True, index=True)
    # Set when a booking holds this slot (DEV-021); guards against closing a booked slot.
    held_by_appointment_id: int | None = Field(default=None, foreign_key="appointments.id")
    created_at: datetime = Field(default_factory=lambda: datetime.now(UTC))
