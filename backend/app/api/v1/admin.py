"""Studio admin panel endpoints (DEV-080), all gated by ``require_admin``.

Availability (DEV-020) and plan assignment (DEV-030) live in their own routers;
this one adds the studio directory, the box-order overview, and promotions.
"""

from fastapi import APIRouter, status

from app.api.deps import AdminUser, SessionDep
from app.repositories.notification_repository import NotificationRepository
from app.repositories.nutrition_plan_repository import NutritionPlanRepository
from app.repositories.order_repository import OrderRepository
from app.repositories.user_repository import UserRepository
from app.schemas.admin import (
    AdminClientRead,
    AdminOrderRead,
    PromotionRequest,
    PromotionResult,
)
from app.services.admin_directory_service import AdminDirectoryService
from app.services.promotion_service import PromotionService

router = APIRouter(prefix="/admin", tags=["admin"])


def _directory(session: SessionDep) -> AdminDirectoryService:
    return AdminDirectoryService(
        UserRepository(session),
        OrderRepository(session),
        NutritionPlanRepository(session),
    )


def _promotions(session: SessionDep) -> PromotionService:
    return PromotionService(UserRepository(session), NotificationRepository(session))


@router.get("/clients", response_model=list[AdminClientRead])
def list_clients(admin: AdminUser, session: SessionDep) -> list[AdminClientRead]:
    return _directory(session).list_clients()


@router.get("/orders", response_model=list[AdminOrderRead])
def list_orders(admin: AdminUser, session: SessionDep) -> list[AdminOrderRead]:
    return _directory(session).list_orders()


@router.post("/promotions", response_model=PromotionResult, status_code=status.HTTP_201_CREATED)
def send_promotion(
    data: PromotionRequest, admin: AdminUser, session: SessionDep
) -> PromotionResult:
    return PromotionResult(recipients=_promotions(session).broadcast(admin.id, data))
