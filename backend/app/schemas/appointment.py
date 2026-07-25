"""Schemas for the client booking flow (Pydantic v2)."""

from datetime import datetime

from pydantic import BaseModel, ConfigDict

from app.models.appointment import AppointmentStatus, VisitType


class SlotPublic(BaseModel):
    """An open slot as shown to a client browsing availability."""

    model_config = ConfigDict(from_attributes=True)

    id: int
    starts_at: datetime
    duration_min: int


class AppointmentCreate(BaseModel):
    slot_id: int
    visit_type: VisitType


class AppointmentRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    slot_id: int | None
    visit_type: VisitType
    scheduled_at: datetime
    status: AppointmentStatus
    price_cents: int
