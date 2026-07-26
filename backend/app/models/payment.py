"""Payment model — a charge against an appointment or a box order (DEV-070).

Records a payment made through a :class:`PaymentProvider`. The backend never
stores raw card data (no PAN/CVV/expiry) — only the provider's opaque charge
reference in ``provider_token`` (ADR-002). Amounts are server-authoritative
integer euro cents (Rule 7), copied from the target entity. Owned per-client
(Rule 4). Exactly one of ``appointment_id`` / ``order_id`` is set.
"""

from datetime import UTC, datetime
from enum import Enum

from sqlmodel import Field, SQLModel


class PaymentMethod(str, Enum):
    apple_pay = "apple_pay"
    google_pay = "google_pay"
    card = "card"


class PaymentStatus(str, Enum):
    succeeded = "succeeded"
    failed = "failed"


class Payment(SQLModel, table=True):
    __tablename__ = "payments"

    id: int | None = Field(default=None, primary_key=True)
    # Ownership: every query MUST filter by client_id for a non-admin caller.
    client_id: int = Field(foreign_key="users.id", index=True)
    appointment_id: int | None = Field(default=None, foreign_key="appointments.id", index=True)
    order_id: int | None = Field(default=None, foreign_key="orders.id", index=True)
    amount_cents: int  # server-authoritative, copied from the target entity
    method: PaymentMethod
    provider: str = Field(max_length=40)  # e.g. "mock"
    # The PSP's charge reference — NEVER a card number (ADR-002).
    provider_token: str = Field(max_length=255)
    status: PaymentStatus = Field(index=True)
    created_at: datetime = Field(default_factory=lambda: datetime.now(UTC))
