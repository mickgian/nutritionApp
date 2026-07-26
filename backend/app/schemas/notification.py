"""Schemas for client notifications (Pydantic v2)."""

from datetime import datetime

from pydantic import BaseModel, ConfigDict

from app.models.notification import NotificationKind


class NotificationRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    kind: NotificationKind
    title: str
    body: str
    icon: str
    is_read: bool
    created_at: datetime
