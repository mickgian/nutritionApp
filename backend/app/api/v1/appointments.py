"""Client booking endpoints: browse open slots, book, list own appointments."""

from datetime import date

from fastapi import APIRouter, Query, status

from app.api.deps import CurrentUser, SessionDep
from app.models.appointment import Appointment
from app.models.availability import AvailabilitySlot
from app.repositories.appointment_repository import AppointmentRepository
from app.repositories.availability_repository import AvailabilityRepository
from app.repositories.credit_repository import CreditRepository
from app.schemas.appointment import (
    AppointmentCreate,
    AppointmentRead,
    AppointmentReschedule,
    CancellationResult,
    SlotPublic,
)
from app.schemas.credit import CreditRead
from app.services.appointment_lifecycle_service import AppointmentLifecycleService
from app.services.appointment_service import AppointmentService

router = APIRouter(tags=["appointments"])


def _service(session: SessionDep) -> AppointmentService:
    return AppointmentService(AppointmentRepository(session), AvailabilityRepository(session))


def _lifecycle(session: SessionDep) -> AppointmentLifecycleService:
    return AppointmentLifecycleService(
        AppointmentRepository(session),
        AvailabilityRepository(session),
        CreditRepository(session),
    )


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


@router.patch("/appointments/{appointment_id}", response_model=AppointmentRead)
def reschedule_appointment(
    appointment_id: int,
    data: AppointmentReschedule,
    current_user: CurrentUser,
    session: SessionDep,
) -> Appointment:
    return _lifecycle(session).reschedule(current_user.id, appointment_id, data)


@router.delete("/appointments/{appointment_id}", response_model=CancellationResult)
def cancel_appointment(
    appointment_id: int,
    current_user: CurrentUser,
    session: SessionDep,
) -> CancellationResult:
    appointment, credit = _lifecycle(session).cancel(current_user.id, appointment_id)
    return CancellationResult(
        appointment_id=appointment.id,
        status=appointment.status,
        credit=CreditRead.model_validate(credit) if credit else None,
    )
