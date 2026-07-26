"""Data access for :class:`User`.

Repositories are the only place that touch the DB session for a given aggregate.
Emails are normalized to lower-case so lookups and uniqueness are case-insensitive.
"""

from sqlmodel import Session, select

from app.models.user import User, UserRole


def normalize_email(email: str) -> str:
    return email.strip().lower()


class UserRepository:
    def __init__(self, session: Session) -> None:
        self.session = session

    def get_by_id(self, user_id: int) -> User | None:
        return self.session.get(User, user_id)

    def list_clients(self) -> list[User]:
        """Active clienti, ordered by name — the studio directory (admin-only)."""
        stmt = (
            select(User)
            .where(User.role == UserRole.cliente, User.is_active == True)  # noqa: E712
            .order_by(User.full_name)
        )
        return list(self.session.exec(stmt).all())

    def get_by_email(self, email: str) -> User | None:
        stmt = select(User).where(User.email == normalize_email(email))
        return self.session.exec(stmt).first()

    def create(self, user: User) -> User:
        self.session.add(user)
        self.session.commit()
        self.session.refresh(user)
        return user
