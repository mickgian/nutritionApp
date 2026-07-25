"""Schemas for the admin availability endpoints (Pydantic v2)."""

from datetime import datetime

from pydantic import BaseModel, ConfigDict, Field


class AvailabilityUpsertRequest(BaseModel):
    """Open or close a slot at a given instant (create-or-update by ``starts_at``)."""

    starts_at: datetime
    duration_min: int = Field(default=60, ge=1, le=480)
    is_open: bool = True


class AvailabilitySlotRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    starts_at: datetime
    duration_min: int
    is_open: bool
