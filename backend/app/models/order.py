"""Order model — a client's meal-box order (single box or subscription).

Reference: the demo's checkout sheet (box singolo €89 / abbonamento €79 a box).
Ordering requires an active nutrition plan (DEV-030). Money is integer euro
cents (Rule 7); datetimes are timezone-aware UTC. Owned per-client (Rule 4):
every query filters by ``client_id``.
"""

from datetime import UTC, datetime
from enum import Enum

from sqlmodel import Field, SQLModel


class OrderFormula(str, Enum):
    single = "single"  # Box singolo · €89
    subscription = "subscription"  # Abbonamento · €79 a box


class PickupSlot(str, Enum):
    mattina = "mattina"
    pomeriggio = "pomeriggio"


class OrderStatus(str, Enum):
    pending_payment = "pending_payment"
    paid = "paid"
    cancelled = "cancelled"


# Server-authoritative price per formula, in integer euro cents (Rule 7).
ORDER_PRICE_CENTS: dict[OrderFormula, int] = {
    OrderFormula.single: 8900,
    OrderFormula.subscription: 7900,
}


class Order(SQLModel, table=True):
    __tablename__ = "orders"

    id: int | None = Field(default=None, primary_key=True)
    # Ownership: every query MUST filter by client_id for a non-admin caller.
    client_id: int = Field(foreign_key="users.id", index=True)
    plan_id: int = Field(foreign_key="nutrition_plans.id", index=True)
    formula: OrderFormula
    pickup: PickupSlot
    status: OrderStatus = Field(default=OrderStatus.pending_payment, index=True)
    price_cents: int
    created_at: datetime = Field(default_factory=lambda: datetime.now(UTC))
