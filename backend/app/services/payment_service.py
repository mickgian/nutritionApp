"""Payment business logic: charge a provider and confirm the target (ADR-002).

A client pays only their own appointment/order (else 404). Amounts are read from
the target entity, never the request (server-authoritative, Rule 7). Paying an
already-paid entity returns the existing payment without re-charging (idempotent).
A declined charge is recorded as ``failed`` and raises 402; the target is left
``pending_payment``. No card data is ever stored — only the provider reference.
"""

import structlog
from fastapi import HTTPException, status

from app.models.appointment import AppointmentStatus
from app.models.order import OrderStatus
from app.models.payment import Payment, PaymentStatus
from app.repositories.appointment_repository import AppointmentRepository
from app.repositories.order_repository import OrderRepository
from app.repositories.payment_repository import PaymentRepository
from app.schemas.payment import PaymentCreate, PaymentTarget
from app.services.payment_provider import MockPaymentProvider, PaymentProvider

logger = structlog.get_logger(__name__)


class PaymentService:
    def __init__(
        self,
        payments: PaymentRepository,
        appointments: AppointmentRepository,
        orders: OrderRepository,
        provider: PaymentProvider | None = None,
    ) -> None:
        self.payments = payments
        self.appointments = appointments
        self.orders = orders
        self.provider = provider or MockPaymentProvider()

    def pay(self, client_id: int, data: PaymentCreate) -> tuple[Payment, bool]:
        """Returns (payment, created); created=False when idempotently returning."""
        if data.target == PaymentTarget.appointment:
            return self._pay_appointment(client_id, data)
        return self._pay_order(client_id, data)

    def _pay_appointment(self, client_id: int, data: PaymentCreate) -> tuple[Payment, bool]:
        appointment = self.appointments.get_by_id(data.target_id)
        if appointment is None or appointment.client_id != client_id:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND, detail="Appuntamento non trovato"
            )
        if appointment.status == AppointmentStatus.cancelled:
            raise HTTPException(
                status_code=status.HTTP_409_CONFLICT, detail="Appuntamento annullato"
            )
        if appointment.status == AppointmentStatus.confirmed:
            return self.payments.succeeded_for_appointment(appointment.id), False
        payment = self._charge(
            client_id, data, appointment.price_cents, appointment_id=appointment.id
        )
        appointment.status = AppointmentStatus.confirmed
        self.appointments.save(appointment)
        return payment, True

    def _pay_order(self, client_id: int, data: PaymentCreate) -> tuple[Payment, bool]:
        order = self.orders.get_by_id(data.target_id)
        if order is None or order.client_id != client_id:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Ordine non trovato")
        if order.status == OrderStatus.cancelled:
            raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="Ordine annullato")
        if order.status == OrderStatus.paid:
            return self.payments.succeeded_for_order(order.id), False
        payment = self._charge(client_id, data, order.price_cents, order_id=order.id)
        order.status = OrderStatus.paid
        self.orders.save(order)
        return payment, True

    def _charge(
        self,
        client_id: int,
        data: PaymentCreate,
        amount_cents: int,
        appointment_id: int | None = None,
        order_id: int | None = None,
    ) -> Payment:
        result = self.provider.charge(data.token, amount_cents)
        succeeded = result.success
        payment = Payment(
            client_id=client_id,
            appointment_id=appointment_id,
            order_id=order_id,
            amount_cents=amount_cents,
            method=data.method,
            provider=self.provider.name,
            provider_token=result.reference,
            status=PaymentStatus.succeeded if succeeded else PaymentStatus.failed,
        )
        created = self.payments.create(payment)
        if not succeeded:
            logger.info(
                "payment_declined",
                operation="charge",
                user_id=client_id,
                payment_id=created.id,
                amount_cents=amount_cents,
            )
            raise HTTPException(
                status_code=status.HTTP_402_PAYMENT_REQUIRED,
                detail="Pagamento non riuscito. Riprova.",
            )
        logger.info(
            "payment_succeeded",
            operation="charge",
            user_id=client_id,
            payment_id=created.id,
            amount_cents=amount_cents,
        )
        return created
