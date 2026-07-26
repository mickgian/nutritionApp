"""Data access for :class:`Credit`. Every query filters by ``client_id`` (Rule 4)."""

from sqlmodel import Session, select

from app.models.credit import Credit


class CreditRepository:
    def __init__(self, session: Session) -> None:
        self.session = session

    def create(self, credit: Credit) -> Credit:
        self.session.add(credit)
        self.session.commit()
        self.session.refresh(credit)
        return credit

    def list_unused_by_client(self, client_id: int) -> list[Credit]:
        """Unused credits for a client; expiry is filtered in the service (tz-safe)."""
        stmt = (
            select(Credit)
            .where(
                Credit.client_id == client_id,
                Credit.is_used == False,  # noqa: E712 (SQL boolean comparison)
            )
            .order_by(Credit.created_at.desc())
        )
        return list(self.session.exec(stmt).all())
