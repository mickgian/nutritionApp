"""Client notifications endpoint: list the caller's own notifications.

``GET /me/notifications`` generates any newly-applicable notifications from the
caller's state (idempotently) and returns their own, newest first (Rule 4).
"""

from fastapi import APIRouter

from app.api.deps import CurrentUser, SessionDep
from app.models.notification import Notification
from app.repositories.appointment_repository import AppointmentRepository
from app.repositories.notification_repository import NotificationRepository
from app.repositories.nutrition_plan_repository import NutritionPlanRepository
from app.repositories.order_repository import OrderRepository
from app.schemas.notification import NotificationRead
from app.services.notification_service import NotificationService

router = APIRouter(tags=["notifications"])


def _service(session: SessionDep) -> NotificationService:
    return NotificationService(
        NotificationRepository(session),
        AppointmentRepository(session),
        NutritionPlanRepository(session),
        OrderRepository(session),
    )


@router.get("/me/notifications", response_model=list[NotificationRead])
def list_my_notifications(current_user: CurrentUser, session: SessionDep) -> list[Notification]:
    return _service(session).list_for(current_user.id)
