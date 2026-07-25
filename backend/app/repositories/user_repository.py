"""Data access for :class:`User`.

Repositories are the only place that touch the DB session for a given aggregate.
Emails are normalized to lower-case so lookups and uniqueness are case-insensitive.
"""

from sqlmodel import Session, select

from app.models.user import User


def normalize_email(email: str) -> str:
    return email.strip().lower()


class UserRepository:
    def __init__(self, session: Session) -> None:
        self.session = session

    def get_by_email(self, email: str) -> User | None:
        stmt = select(User).where(User.email == normalize_email(email))
        return self.session.exec(stmt).first()

    def create(self, user: User) -> User:
        self.session.add(user)
        self.session.commit()
        self.session.refresh(user)
        return user
