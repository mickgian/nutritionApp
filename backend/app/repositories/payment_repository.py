"""Data access for :class:`Payment`. Every query filters by ``client_id`` (Rule 4)."""

from sqlmodel import Session, select

from app.models.payment import Payment, PaymentStatus


class PaymentRepository:
    def __init__(self, session: Session) -> None:
        self.session = session

    def create(self, payment: Payment) -> Payment:
        self.session.add(payment)
        self.session.commit()
        self.session.refresh(payment)
        return payment

    def succeeded_for_appointment(self, appointment_id: int) -> Payment | None:
        stmt = select(Payment).where(
            Payment.appointment_id == appointment_id,
            Payment.status == PaymentStatus.succeeded,
        )
        return self.session.exec(stmt).first()

    def succeeded_for_order(self, order_id: int) -> Payment | None:
        stmt = select(Payment).where(
            Payment.order_id == order_id,
            Payment.status == PaymentStatus.succeeded,
        )
        return self.session.exec(stmt).first()
