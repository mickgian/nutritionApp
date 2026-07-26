"""Payment endpoint: confirm an appointment or box order (DEV-070).

``POST /payments`` charges the provider for the caller's own target and confirms
it. 201 on a new payment, 200 when idempotently returning an existing one. The
amount is server-authoritative (read from the entity); no card data is stored.
"""

from fastapi import APIRouter, Response, status

from app.api.deps import CurrentUser, SessionDep
from app.models.payment import Payment
from app.repositories.appointment_repository import AppointmentRepository
from app.repositories.order_repository import OrderRepository
from app.repositories.payment_repository import PaymentRepository
from app.schemas.payment import PaymentCreate, PaymentRead
from app.services.payment_service import PaymentService

router = APIRouter(tags=["payments"])


def _service(session: SessionDep) -> PaymentService:
    return PaymentService(
        PaymentRepository(session),
        AppointmentRepository(session),
        OrderRepository(session),
    )


@router.post("/payments", response_model=PaymentRead, status_code=status.HTTP_201_CREATED)
def create_payment(
    data: PaymentCreate,
    current_user: CurrentUser,
    session: SessionDep,
    response: Response,
) -> Payment:
    payment, created = _service(session).pay(current_user.id, data)
    if not created:
        response.status_code = status.HTTP_200_OK
    return payment
