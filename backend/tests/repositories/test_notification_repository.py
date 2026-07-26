"""Repository test for the idempotency race guard (DEV-060).

``create_missing`` must not raise if a concurrent insert already wrote the same
``(client_id, dedupe_key)`` — the unique constraint fires and the guard swallows
it (Rule 6: no 500 on an expected race).
"""

from sqlmodel import Session

from app.models.notification import Notification, NotificationKind
from app.models.user import UserRole
from app.repositories.notification_repository import NotificationRepository


def _notification(client_id: int, key: str, title: str) -> Notification:
    return Notification(
        client_id=client_id,
        kind=NotificationKind.order_deadline,
        title=title,
        body="corpo",
        icon="🍱",
        dedupe_key=key,
    )


def test_create_missing_ignores_duplicate_dedupe_key(session: Session, make_user):
    cliente = make_user("dupkey@example.com", UserRole.cliente)
    repo = NotificationRepository(session)
    repo.create_missing([_notification(cliente.id, "order_deadline:1", "Primo")])
    # A second insert with the same key (simulating a race) must not raise.
    repo.create_missing([_notification(cliente.id, "order_deadline:1", "Secondo")])
    stored = repo.list_by_client(cliente.id)
    assert len(stored) == 1
    assert stored[0].title == "Primo"
