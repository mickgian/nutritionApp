"""Client weekly meal box endpoint: GET /api/v1/me/box?week=."""

from fastapi import APIRouter, Query

from app.api.deps import CurrentUser, SessionDep
from app.repositories.box_repository import BoxRepository
from app.repositories.meal_repository import MealRepository
from app.repositories.nutrition_plan_repository import NutritionPlanRepository
from app.schemas.box import BoxRead
from app.services.box_service import BoxService

router = APIRouter(tags=["box"])


def _service(session: SessionDep) -> BoxService:
    return BoxService(
        NutritionPlanRepository(session),
        MealRepository(session),
        BoxRepository(session),
    )


@router.get("/me/box", response_model=BoxRead)
def get_my_box(
    current_user: CurrentUser,
    session: SessionDep,
    week: int = Query(default=0, ge=0),
) -> BoxRead:
    return _service(session).get_box(current_user.id, week)
