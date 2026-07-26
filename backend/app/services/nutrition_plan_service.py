"""Nutrition plan business logic: assign (studio) and read the client's own plan.

The studio assigns a plan to a *cliente*; assigning a new one deactivates any
previous active plan (one active plan per client, ADR-001). A client reads only
their own active plan; no active plan raises 404 so the client renders a locked
box (an expected condition, not a 500). Ownership is enforced by the caller
passing ``client_id`` from the JWT.
"""

import structlog
from fastapi import HTTPException, status

from app.models.nutrition_plan import NutritionPlan
from app.models.user import UserRole
from app.repositories.nutrition_plan_repository import NutritionPlanRepository
from app.repositories.user_repository import UserRepository
from app.schemas.nutrition_plan import PlanAssignRequest

logger = structlog.get_logger(__name__)


class NutritionPlanService:
    def __init__(self, plans: NutritionPlanRepository, users: UserRepository) -> None:
        self.plans = plans
        self.users = users

    def assign(self, admin_id: int, client_id: int, data: PlanAssignRequest) -> NutritionPlan:
        target = self.users.get_by_id(client_id)
        if target is None or target.role != UserRole.cliente:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Cliente non trovato")
        self.plans.deactivate_active(client_id)
        plan = NutritionPlan(
            client_id=client_id,
            name=data.name,
            daily_kcal=data.daily_kcal,
            weeks=data.weeks,
            is_active=True,
            assigned_by=admin_id,
        )
        created = self.plans.create(plan)
        logger.info(
            "plan_assigned",
            operation="assign_plan",
            user_id=admin_id,
            client_id=client_id,
            plan_id=created.id,
        )
        return created

    def get_active(self, client_id: int) -> NutritionPlan:
        plan = self.plans.get_active_by_client(client_id)
        if plan is None:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Nessun piano attivo")
        return plan
