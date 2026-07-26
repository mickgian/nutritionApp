"""Schemas for meal-box orders (Pydantic v2)."""

from datetime import datetime

from pydantic import BaseModel, ConfigDict

from app.models.order import OrderFormula, OrderStatus, PickupSlot


class OrderCreateRequest(BaseModel):
    """A client places a box order: formula + pickup slot (both required)."""

    formula: OrderFormula
    pickup: PickupSlot


class OrderRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    formula: OrderFormula
    pickup: PickupSlot
    status: OrderStatus
    price_cents: int
    created_at: datetime
