"""Appointment model — a nutrition consultation booked by a client.

Reference: the demo's booking wizard (prima visita €90 / controllo €50).
This is a starter model to make the scaffold runnable; the full domain schema
is designed by @primo in the tasks under docs/tasks/.
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


class Appointment(SQLModel, table=True):
    __tablename__ = "appointments"

    id: int | None = Field(default=None, primary_key=True)
    # Ownership: every query MUST filter by client_id for a non-admin caller.
    client_id: int = Field(foreign_key="users.id", index=True)
    visit_type: VisitType
    scheduled_at: datetime = Field(index=True)
    status: AppointmentStatus = Field(default=AppointmentStatus.pending_payment, index=True)
    price_eur: int  # whole euros to avoid float money; refine to cents in domain design
    created_at: datetime = Field(default_factory=lambda: datetime.now(UTC))
