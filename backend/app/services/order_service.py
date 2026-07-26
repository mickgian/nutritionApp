"""Meal-box order business logic: place an order and list the client's own.

Ordering requires an active nutrition plan (no plan → 403, an expected
condition, not a 500 — Rule 6). A client may hold only one active subscription
at a time (a second → 409). Prices are server-authoritative (Rule 7).
Ownership is enforced by the caller passing ``client_id`` from the JWT.
"""

import structlog
from fastapi import HTTPException, status

from app.models.order import ORDER_PRICE_CENTS, Order, OrderFormula, OrderStatus
from app.repositories.nutrition_plan_repository import NutritionPlanRepository
from app.repositories.order_repository import OrderRepository
from app.schemas.order import OrderCreateRequest

logger = structlog.get_logger(__name__)


class OrderService:
    def __init__(self, orders: OrderRepository, plans: NutritionPlanRepository) -> None:
        self.orders = orders
        self.plans = plans

    def create(self, client_id: int, data: OrderCreateRequest) -> Order:
        plan = self.plans.get_active_by_client(client_id)
        if plan is None:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN, detail="Serve prima un piano"
            )
        if data.formula == OrderFormula.subscription and self.orders.get_active_subscription(
            client_id
        ):
            raise HTTPException(
                status_code=status.HTTP_409_CONFLICT, detail="Hai già un abbonamento attivo"
            )
        order = Order(
            client_id=client_id,
            plan_id=plan.id,
            formula=data.formula,
            pickup=data.pickup,
            status=OrderStatus.pending_payment,
            price_cents=ORDER_PRICE_CENTS[data.formula],
        )
        created = self.orders.create(order)
        logger.info(
            "order_created",
            operation="create_order",
            user_id=client_id,
            order_id=created.id,
            formula=data.formula.value,
            price_cents=created.price_cents,
        )
        return created

    def list_own(self, client_id: int) -> list[Order]:
        return self.orders.list_by_client(client_id)
