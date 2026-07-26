"""Data access for :class:`Notification`. Every query filters by ``client_id``."""

import structlog
from sqlalchemy.exc import IntegrityError
from sqlmodel import Session, select

from app.models.notification import Notification

logger = structlog.get_logger(__name__)


class NotificationRepository:
    def __init__(self, session: Session) -> None:
        self.session = session

    def list_by_client(self, client_id: int) -> list[Notification]:
        stmt = (
            select(Notification)
            .where(Notification.client_id == client_id)
            .order_by(Notification.created_at.desc())
        )
        return list(self.session.exec(stmt).all())

    def existing_keys(self, client_id: int) -> set[str]:
        stmt = select(Notification.dedupe_key).where(Notification.client_id == client_id)
        return set(self.session.exec(stmt).all())

    def mark_all_read(self, client_id: int) -> int:
        """Mark the client's unread notifications read; returns how many changed."""
        stmt = select(Notification).where(
            Notification.client_id == client_id,
            Notification.is_read == False,  # noqa: E712 (SQL boolean comparison)
        )
        rows = list(self.session.exec(stmt).all())
        for notification in rows:
            notification.is_read = True
            self.session.add(notification)
        if rows:
            self.session.commit()
        return len(rows)

    def create_missing(self, notifications: list[Notification]) -> None:
        """Insert new notifications; a unique-key race just means it already exists."""
        if not notifications:
            return
        for notification in notifications:
            self.session.add(notification)
        try:
            self.session.commit()
        except IntegrityError:
            # A concurrent request already created a duplicate dedupe_key; not an error.
            self.session.rollback()
            logger.info(
                "notification_create_race",
                operation="create_missing",
                client_id=notifications[0].client_id,
                error_type="IntegrityError",
            )
