"""Meal catalog read logic: fetch a single meal for the detail view (DEV-032).

Meals are shared reference data (not per-user owned); an unknown id is an
expected condition and raises 404, never a 500.
"""

from fastapi import HTTPException, status

from app.models.meal import Meal
from app.repositories.meal_repository import MealRepository


class MealService:
    def __init__(self, meals: MealRepository) -> None:
        self.meals = meals

    def get(self, meal_id: int) -> Meal:
        meal = self.meals.get_by_id(meal_id)
        if meal is None:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Pasto non trovato")
        return meal
