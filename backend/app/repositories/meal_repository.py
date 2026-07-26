"""Data access for the shared :class:`Meal` catalog (reference data)."""

from sqlmodel import Session, select

from app.models.meal import Meal, MealSlot


class MealRepository:
    def __init__(self, session: Session) -> None:
        self.session = session

    def count(self) -> int:
        return len(list(self.session.exec(select(Meal)).all()))

    def list_by_slot(self, slot: MealSlot) -> list[Meal]:
        stmt = select(Meal).where(Meal.slot == slot).order_by(Meal.id)
        return list(self.session.exec(stmt).all())

    def get_by_id(self, meal_id: int) -> Meal | None:
        return self.session.get(Meal, meal_id)

    def bulk_create(self, meals: list[Meal]) -> None:
        for meal in meals:
            self.session.add(meal)
        self.session.commit()
