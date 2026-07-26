"""Schemas for the professional profile & reviews endpoint (Pydantic v2)."""

from datetime import datetime

from pydantic import BaseModel, ConfigDict

from app.models.professional import Professional
from app.models.review import Review


class ReviewRead(BaseModel):
    """A single review as shown on the Consulenza card."""

    model_config = ConfigDict(from_attributes=True)

    id: int
    author_name: str
    rating: int
    body: str
    created_at: datetime


class ProfessionalRead(BaseModel):
    """The studio nutritionist plus her reviews.

    ``rating`` is the human-facing average (``rating_avg`` is stored as an int
    x100 to stay float-free); the client renders it as stars.
    """

    model_config = ConfigDict(from_attributes=True)

    id: int
    full_name: str
    title: str
    bio: str
    rating: float
    reviews_count: int
    reviews: list[ReviewRead]

    @classmethod
    def build(cls, professional: Professional, reviews: list[Review]) -> "ProfessionalRead":
        return cls(
            id=professional.id,
            full_name=professional.full_name,
            title=professional.title,
            bio=professional.bio,
            rating=round(professional.rating_avg / 100, 2),
            reviews_count=professional.reviews_count,
            reviews=[ReviewRead.model_validate(r) for r in reviews],
        )
