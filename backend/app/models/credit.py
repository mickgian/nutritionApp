"""Credit model — a store credit issued to a client (e.g. on a >48h cancellation).

Cancelling an appointment more than 48h before its start refunds the price as a
6-month store credit (DEV-050). Owned per-client (Rule 4); money is integer euro
cents (Rule 7); datetimes are timezone-aware UTC.
"""

from datetime import UTC, datetime

from sqlmodel import Field, SQLModel

# A cancellation more than this many hours before the visit earns a credit.
CANCELLATION_CREDIT_WINDOW_HOURS = 48
# A credit is valid for six months from issue.
CREDIT_VALIDITY_DAYS = 180


class Credit(SQLModel, table=True):
    __tablename__ = "credits"

    id: int | None = Field(default=None, primary_key=True)
    # Ownership: every query MUST filter by client_id for a non-admin caller.
    client_id: int = Field(foreign_key="users.id", index=True)
    amount_cents: int
    reason: str = Field(max_length=200)  # Italian, e.g. "Annullamento appuntamento"
    source_appointment_id: int | None = Field(default=None, foreign_key="appointments.id")
    is_used: bool = Field(default=False, index=True)
    created_at: datetime = Field(default_factory=lambda: datetime.now(UTC))
    expires_at: datetime
