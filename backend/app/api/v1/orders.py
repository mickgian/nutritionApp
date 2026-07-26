"""Meal-box order endpoints: a client places and lists their own orders.

``POST /orders`` requires an active plan (403 otherwise); ``GET /orders``
returns only the caller's own orders (Rule 4 — per-user isolation).
"""

from fastapi import APIRouter, status

from app.api.deps import CurrentUser, SessionDep
from app.models.order import Order
from app.repositories.nutrition_plan_repository import NutritionPlanRepository
from app.repositories.order_repository import OrderRepository
from app.schemas.order import OrderCreateRequest, OrderRead
from app.services.order_service import OrderService

router = APIRouter()


def _service(session: SessionDep) -> OrderService:
    return OrderService(OrderRepository(session), NutritionPlanRepository(session))


@router.post(
    "/orders", response_model=OrderRead, status_code=status.HTTP_201_CREATED, tags=["orders"]
)
def create_order(data: OrderCreateRequest, current_user: CurrentUser, session: SessionDep) -> Order:
    return _service(session).create(current_user.id, data)


@router.get("/orders", response_model=list[OrderRead], tags=["orders"])
def list_orders(current_user: CurrentUser, session: SessionDep) -> list[Order]:
    return _service(session).list_own(current_user.id)
