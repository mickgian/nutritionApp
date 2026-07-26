"""Studio promotions (DEV-080): broadcast one notification to every client.

A promotion is the studio's manual push. It fans out as a ``promotion``
notification per active cliente, sharing a single ``dedupe_key`` (unique per
client) so the same broadcast is never duplicated in a client's feed. Delivery
is the existing lazy-read notifications feed; real push is a later integration.
"""

from datetime import UTC, datetime

import structlog

from app.models.notification import Notification, NotificationKind
from app.repositories.notification_repository import NotificationRepository
from app.repositories.user_repository import UserRepository
from app.schemas.admin import PromotionRequest

logger = structlog.get_logger(__name__)


class PromotionService:
    def __init__(self, users: UserRepository, notifications: NotificationRepository) -> None:
        self.users = users
        self.notifications = notifications

    def broadcast(self, admin_id: int, data: PromotionRequest) -> int:
        clients = self.users.list_clients()
        if not clients:
            return 0
        dedupe_key = f"promo:{datetime.now(UTC).isoformat()}"
        messages = [
            Notification(
                client_id=client.id,
                kind=NotificationKind.promotion,
                title=data.title,
                body=data.body,
                icon=data.icon,
                dedupe_key=dedupe_key,
            )
            for client in clients
        ]
        self.notifications.create_missing(messages)
        logger.info(
            "promotion_broadcast",
            operation="promotion_broadcast",
            user_id=admin_id,
            recipients=len(messages),
        )
        return len(messages)
