"""Review model — a review on the studio professional (shown on Consulenza).

``author_id`` links a real client's account when present; ``author_name`` is the
display name captured at review time. Keeping the name denormalized (ADR-001
Amendment 1) lets seeded demo reviews — and reviews from later-deleted accounts
(GDPR) — still render without leaking or losing the account. ``rating`` is 1-5.
"""

from datetime import UTC, datetime

from sqlmodel import Field, SQLModel


class Review(SQLModel, table=True):
    __tablename__ = "reviews"

    id: int | None = Field(default=None, primary_key=True)
    professional_id: int = Field(foreign_key="professionals.id", index=True)
    author_id: int | None = Field(default=None, foreign_key="users.id")
    author_name: str = Field(max_length=120)
    rating: int  # 1-5
    body: str = Field(max_length=1000)
    created_at: datetime = Field(default_factory=lambda: datetime.now(UTC))
