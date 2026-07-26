"""Professional model — the studio's nutritionist (single studio).

Reference: the Consulenza card (Dott.ssa Serra). One row is expected; we model
it explicitly (ADR-001) so bio/rating/reviews have a home. ``rating_avg`` is
stored as an integer (stars x100, e.g. 487 = 4.87) to keep rating math
float-free (Rule 7); ``reviews_count`` is a denormalized counter maintained by
the reviews service.
"""

from datetime import UTC, datetime

from sqlmodel import Field, SQLModel


class Professional(SQLModel, table=True):
    __tablename__ = "professionals"

    id: int | None = Field(default=None, primary_key=True)
    # Optional link to the admin/nutritionist login (nullable).
    user_id: int | None = Field(default=None, foreign_key="users.id")
    full_name: str = Field(max_length=120)
    title: str = Field(max_length=120)
    bio: str = Field(max_length=2000)
    rating_avg: int = Field(default=0)  # stars x100 (487 = 4.87)
    reviews_count: int = Field(default=0)
    created_at: datetime = Field(default_factory=lambda: datetime.now(UTC))
