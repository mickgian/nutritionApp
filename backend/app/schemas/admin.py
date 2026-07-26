"""Schemas for the studio admin panel (DEV-080). Studio-only, Italian-facing."""

from datetime import datetime

from pydantic import BaseModel, Field

from app.models.order import OrderFormula, OrderStatus, PickupSlot


class AdminClientRead(BaseModel):
    """A client row for the studio directory, with their active plan name (if any)."""

    id: int
    full_name: str
    email: str
    active_plan: str | None = None


class AdminOrderRead(BaseModel):
    """A box order across all clients, annotated with the ordering client."""

    id: int
    client_id: int
    client_name: str
    client_email: str
    formula: OrderFormula
    pickup: PickupSlot
    status: OrderStatus
    price_cents: int
    created_at: datetime


class PromotionRequest(BaseModel):
    """A studio broadcast pushed to every active client as a notification."""

    title: str = Field(min_length=1, max_length=200)
    body: str = Field(min_length=1, max_length=1000)
    icon: str = Field(default="📣", min_length=1, max_length=16)


class PromotionResult(BaseModel):
    """How many clients received the broadcast."""

    recipients: int
