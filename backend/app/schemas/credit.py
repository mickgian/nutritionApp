"""Schemas for store credits (Pydantic v2)."""

from datetime import datetime

from pydantic import BaseModel, ConfigDict


class CreditRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    amount_cents: int
    reason: str
    source_appointment_id: int | None
    created_at: datetime
    expires_at: datetime
