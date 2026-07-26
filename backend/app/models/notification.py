"""Notification model — a behavior-profiled message shown to a client.

Notifications are generated from the client's own state by the rules service
(DEV-060): appointment reminder, order deadline, box ready, post-consult box
proposal. Push delivery is a later integration. Owned per-client (Rule 4). Each
carries a ``dedupe_key`` so the same event is never listed twice (unique per
client). Datetimes are timezone-aware UTC.
"""

from datetime import UTC, datetime
from enum import Enum

from sqlmodel import Field, SQLModel, UniqueConstraint


class NotificationKind(str, Enum):
    appointment_reminder = "appointment_reminder"
    order_deadline = "order_deadline"
    box_ready = "box_ready"
    box_proposal = "box_proposal"
    promotion = "promotion"  # studio broadcast (DEV-080)


class Notification(SQLModel, table=True):
    __tablename__ = "notifications"
    __table_args__ = (
        UniqueConstraint("client_id", "dedupe_key", name="uq_notifications_client_key"),
    )

    id: int | None = Field(default=None, primary_key=True)
    # Ownership: every query MUST filter by client_id for a non-admin caller.
    client_id: int = Field(foreign_key="users.id", index=True)
    kind: NotificationKind
    title: str = Field(max_length=200)  # Italian
    body: str = Field(max_length=1000)  # Italian
    icon: str = Field(max_length=16)  # emoji
    # Natural key of the event that produced this notification (idempotency).
    dedupe_key: str = Field(max_length=200, index=True)
    is_read: bool = Field(default=False, index=True)
    created_at: datetime = Field(default_factory=lambda: datetime.now(UTC))
