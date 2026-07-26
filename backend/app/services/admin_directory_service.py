"""Read models for the studio admin panel (DEV-080): clients + box orders.

Studio-only — the router guards every entry point with ``require_admin``. This
service only reads; it filters by no ``client_id`` on purpose (the admin sees
the whole studio), which is exactly why it must never be reachable by a cliente.
"""

from app.repositories.nutrition_plan_repository import NutritionPlanRepository
from app.repositories.order_repository import OrderRepository
from app.repositories.user_repository import UserRepository
from app.schemas.admin import AdminClientRead, AdminOrderRead


class AdminDirectoryService:
    def __init__(
        self,
        users: UserRepository,
        orders: OrderRepository,
        plans: NutritionPlanRepository,
    ) -> None:
        self.users = users
        self.orders = orders
        self.plans = plans

    def list_clients(self) -> list[AdminClientRead]:
        plan_names = self.plans.active_names_by_client()
        return [
            AdminClientRead(
                id=client.id,
                full_name=client.full_name,
                email=client.email,
                active_plan=plan_names.get(client.id),
            )
            for client in self.users.list_clients()
        ]

    def list_orders(self) -> list[AdminOrderRead]:
        return [
            AdminOrderRead(
                id=order.id,
                client_id=order.client_id,
                client_name=name,
                client_email=email,
                formula=order.formula,
                pickup=order.pickup,
                status=order.status,
                price_cents=order.price_cents,
                created_at=order.created_at,
            )
            for order, name, email in self.orders.list_all_with_client()
        ]
