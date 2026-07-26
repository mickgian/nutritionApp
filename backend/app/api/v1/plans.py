"""Nutrition plan endpoints: studio assigns (admin), client reads own plan.

``POST /admin/clients/{client_id}/plan`` is admin-only; ``GET /me/plan`` returns
the caller's own active plan (404 → the client renders a locked box, DEV-033).
"""

from fastapi import APIRouter, status

from app.api.deps import AdminUser, CurrentUser, SessionDep
from app.models.nutrition_plan import NutritionPlan
from app.repositories.nutrition_plan_repository import NutritionPlanRepository
from app.repositories.user_repository import UserRepository
from app.schemas.nutrition_plan import PlanAssignRequest, PlanRead
from app.services.nutrition_plan_service import NutritionPlanService

router = APIRouter()


def _service(session: SessionDep) -> NutritionPlanService:
    return NutritionPlanService(NutritionPlanRepository(session), UserRepository(session))


@router.post(
    "/admin/clients/{client_id}/plan",
    response_model=PlanRead,
    status_code=status.HTTP_201_CREATED,
    tags=["admin"],
)
def assign_plan(
    client_id: int,
    data: PlanAssignRequest,
    admin: AdminUser,
    session: SessionDep,
) -> NutritionPlan:
    return _service(session).assign(admin.id, client_id, data)


@router.get("/me/plan", response_model=PlanRead, tags=["plans"])
def get_my_plan(current_user: CurrentUser, session: SessionDep) -> NutritionPlan:
    return _service(session).get_active(current_user.id)
