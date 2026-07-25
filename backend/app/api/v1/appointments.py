"""Client booking endpoints: browse open slots, book, list own appointments."""

from datetime import date

from fastapi import APIRouter, Query, status

from app.api.deps import CurrentUser, SessionDep
from app.models.appointment import Appointment
from app.models.availability import AvailabilitySlot
from app.repositories.appointment_repository import AppointmentRepository
from app.repositories.availability_repository import AvailabilityRepository
from app.schemas.appointment import AppointmentCreate, AppointmentRead, SlotPublic
from app.services.appointment_service import AppointmentService

router = APIRouter(tags=["appointments"])


def _service(session: SessionDep) -> AppointmentService:
    return AppointmentService(AppointmentRepository(session), AvailabilityRepository(session))


@router.get("/availability", response_model=list[SlotPublic])
def list_open_slots(
    current_user: CurrentUser,
    session: SessionDep,
    from_: date = Query(alias="from"),
    to: date = Query(),
) -> list[AvailabilitySlot]:
    return _service(session).list_open_slots(from_, to)


@router.post("/appointments", response_model=AppointmentRead, status_code=status.HTTP_201_CREATED)
def book_appointment(
    data: AppointmentCreate,
    current_user: CurrentUser,
    session: SessionDep,
) -> Appointment:
    return _service(session).book(current_user.id, data)


@router.get("/appointments", response_model=list[AppointmentRead])
def list_my_appointments(
    current_user: CurrentUser,
    session: SessionDep,
) -> list[Appointment]:
    return _service(session).list_mine(current_user.id)
