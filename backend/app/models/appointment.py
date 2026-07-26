"""Appointment model — a nutrition consultation booked by a client.

Reference: the demo's booking wizard (prima visita €90 / controllo €50).
Money is integer euro cents (Rule 7); datetimes are timezone-aware UTC.
"""

from datetime import UTC, datetime
from enum import Enum

from sqlmodel import Field, SQLModel


class VisitType(str, Enum):
    prima = "prima"  # Prima visita · 60 min · €90
    controllo = "controllo"  # Visita di controllo · 30 min · €50


class AppointmentStatus(str, Enum):
    pending_payment = "pending_payment"
    confirmed = "confirmed"
    cancelled = "cancelled"


# Server-authoritative price per visit type, in integer euro cents.
VISIT_PRICE_CENTS: dict[VisitType, int] = {
    VisitType.prima: 9000,
    VisitType.controllo: 5000,
}


class Appointment(SQLModel, table=True):
    __tablename__ = "appointments"

    id: int | None = Field(default=None, primary_key=True)
    # Ownership: every query MUST filter by client_id for a non-admin caller.
    client_id: int = Field(foreign_key="users.id", index=True)
    slot_id: int | None = Field(default=None, foreign_key="availability_slots.id", index=True)
    visit_type: VisitType
    scheduled_at: datetime = Field(index=True)
    status: AppointmentStatus = Field(default=AppointmentStatus.pending_payment, index=True)
    price_cents: int
    created_at: datetime = Field(default_factory=lambda: datetime.now(UTC))
