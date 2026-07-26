"""Data access for :class:`Order`. Every query filters by ``client_id`` (Rule 4)."""

from sqlmodel import Session, select

from app.models.order import Order, OrderFormula, OrderStatus


class OrderRepository:
    def __init__(self, session: Session) -> None:
        self.session = session

    def list_by_client(self, client_id: int) -> list[Order]:
        stmt = select(Order).where(Order.client_id == client_id).order_by(Order.created_at.desc())
        return list(self.session.exec(stmt).all())

    def get_active_subscription(self, client_id: int) -> Order | None:
        """The client's current subscription, if any (not cancelled)."""
        stmt = select(Order).where(
            Order.client_id == client_id,
            Order.formula == OrderFormula.subscription,
            Order.status != OrderStatus.cancelled,
        )
        return self.session.exec(stmt).first()

    def create(self, order: Order) -> Order:
        self.session.add(order)
        self.session.commit()
        self.session.refresh(order)
        return order
