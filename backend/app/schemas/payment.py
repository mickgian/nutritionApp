"""Schemas for payments (Pydantic v2). Amounts are server-authoritative — the
pay request carries no amount field (ADR-002)."""

from datetime import datetime
from enum import Enum

from pydantic import BaseModel, ConfigDict, Field

from app.models.payment import PaymentMethod, PaymentStatus


class PaymentTarget(str, Enum):
    appointment = "appointment"
    order = "order"


class PaymentCreate(BaseModel):
    """Pay an appointment or order with a provider token (never a PAN)."""

    target: PaymentTarget
    target_id: int
    method: PaymentMethod
    token: str = Field(min_length=1, max_length=255)


class PaymentRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    appointment_id: int | None
    order_id: int | None
    amount_cents: int
    method: PaymentMethod
    provider: str
    provider_token: str
    status: PaymentStatus
    created_at: datetime
