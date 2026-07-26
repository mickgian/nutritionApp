"""Store credit business logic: list a client's own active credits.

A credit is *active* when it is unused and not yet expired. Expiry is evaluated
here (tz-aware UTC) rather than in SQL so SQLite and PostgreSQL agree. Ownership
is enforced by the caller passing ``client_id`` from the JWT.
"""

from datetime import UTC, datetime

from app.core.timeutils import to_utc
from app.models.credit import Credit
from app.repositories.credit_repository import CreditRepository


class CreditService:
    def __init__(self, credit_repo: CreditRepository) -> None:
        self.credits = credit_repo

    def list_active(self, client_id: int) -> list[Credit]:
        now = datetime.now(UTC)
        return [
            credit
            for credit in self.credits.list_unused_by_client(client_id)
            if to_utc(credit.expires_at) > now
        ]
