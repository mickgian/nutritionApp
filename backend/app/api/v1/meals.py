"""Meal detail endpoint: GET /api/v1/meals/{meal_id} (from the box, DEV-032)."""

from fastapi import APIRouter

from app.api.deps import CurrentUser, SessionDep
from app.repositories.meal_repository import MealRepository
from app.schemas.box import MealRead
from app.services.meal_service import MealService

router = APIRouter(tags=["meals"])


@router.get("/meals/{meal_id}", response_model=MealRead)
def get_meal(meal_id: int, current_user: CurrentUser, session: SessionDep) -> MealRead:
    return MealService(MealRepository(session)).get(meal_id)
