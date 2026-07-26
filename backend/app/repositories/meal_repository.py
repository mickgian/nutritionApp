"""Data access for the shared :class:`Meal` catalog (reference data)."""

from collections.abc import Iterable

from sqlalchemy import func
from sqlmodel import Session, select

from app.models.meal import Meal, MealSlot


class MealRepository:
    def __init__(self, session: Session) -> None:
        self.session = session

    def count(self) -> int:
        # COUNT(*) in the database — never loads every row just to size the table.
        return self.session.exec(select(func.count()).select_from(Meal)).one()

    def list_by_slot(self, slot: MealSlot) -> list[Meal]:
        stmt = select(Meal).where(Meal.slot == slot).order_by(Meal.id)
        return list(self.session.exec(stmt).all())

    def get_by_id(self, meal_id: int) -> Meal | None:
        return self.session.get(Meal, meal_id)

    def list_by_ids(self, meal_ids: Iterable[int]) -> list[Meal]:
        """Batch-fetch meals by id in one query (avoids the per-item N+1)."""
        ids = list(meal_ids)
        if not ids:
            return []
        return list(self.session.exec(select(Meal).where(Meal.id.in_(ids))).all())

    def bulk_create(self, meals: list[Meal]) -> None:
        for meal in meals:
            self.session.add(meal)
        self.session.commit()
