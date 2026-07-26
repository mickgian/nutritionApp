"""Data access for :class:`Professional` and its :class:`Review` rows."""

from sqlmodel import Session, select

from app.models.professional import Professional
from app.models.review import Review


class ProfessionalRepository:
    def __init__(self, session: Session) -> None:
        self.session = session

    def get_singleton(self) -> Professional | None:
        """The studio's single professional (lowest id wins if ever duplicated)."""
        stmt = select(Professional).order_by(Professional.id).limit(1)
        return self.session.exec(stmt).first()

    def all_professionals(self) -> list[Professional]:
        return list(self.session.exec(select(Professional)).all())

    def list_reviews(self, professional_id: int) -> list[Review]:
        stmt = (
            select(Review)
            .where(Review.professional_id == professional_id)
            .order_by(Review.created_at.desc())
        )
        return list(self.session.exec(stmt).all())

    def save(self, professional: Professional) -> Professional:
        self.session.add(professional)
        self.session.commit()
        self.session.refresh(professional)
        return professional

    def add_reviews(self, reviews: list[Review]) -> None:
        for review in reviews:
            self.session.add(review)
        self.session.commit()
