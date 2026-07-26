"""Data access for :class:`Order`. Client queries filter by ``client_id`` (Rule 4).

The ``list_all_with_client`` view is studio-wide and reached only through
admin-guarded routes (DEV-080), never by a cliente.
"""

from sqlmodel import Session, select

from app.models.order import Order, OrderFormula, OrderStatus
from app.models.user import User


class OrderRepository:
    def __init__(self, session: Session) -> None:
        self.session = session

    def list_by_client(self, client_id: int) -> list[Order]:
        stmt = select(Order).where(Order.client_id == client_id).order_by(Order.created_at.desc())
        return list(self.session.exec(stmt).all())

    def list_all_with_client(self) -> list[tuple[Order, str, str]]:
        """Every order joined to its client's name and email (studio view)."""
        stmt = (
            select(Order, User.full_name, User.email)
            .join(User, User.id == Order.client_id)
            .order_by(Order.created_at.desc())
        )
        return [(order, name, email) for order, name, email in self.session.exec(stmt).all()]

    def get_active_subscription(self, client_id: int) -> Order | None:
        """The client's current subscription, if any (not cancelled)."""
        stmt = select(Order).where(
            Order.client_id == client_id,
            Order.formula == OrderFormula.subscription,
            Order.status != OrderStatus.cancelled,
        )
        return self.session.exec(stmt).first()

    def get_by_id(self, order_id: int) -> Order | None:
        return self.session.get(Order, order_id)

    def create(self, order: Order) -> Order:
        self.session.add(order)
        self.session.commit()
        self.session.refresh(order)
        return order

    def save(self, order: Order) -> Order:
        self.session.add(order)
        self.session.commit()
        self.session.refresh(order)
        return order
