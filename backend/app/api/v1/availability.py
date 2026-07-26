"""Admin availability endpoints (studio-only): open/close and list slots."""

from datetime import date

from fastapi import APIRouter, Query

from app.api.deps import AdminUser, SessionDep
from app.models.availability import AvailabilitySlot
from app.repositories.availability_repository import AvailabilityRepository
from app.schemas.availability import AvailabilitySlotRead, AvailabilityUpsertRequest
from app.services.availability_service import AvailabilityService

router = APIRouter(prefix="/admin/availability", tags=["admin"])


def _service(session: SessionDep) -> AvailabilityService:
    return AvailabilityService(AvailabilityRepository(session))


@router.get("", response_model=list[AvailabilitySlotRead])
def list_availability(
    admin: AdminUser,
    session: SessionDep,
    from_: date = Query(alias="from"),
    to: date = Query(),
) -> list[AvailabilitySlot]:
    return _service(session).list_range(from_, to)


@router.put("", response_model=AvailabilitySlotRead)
def set_availability(
    data: AvailabilityUpsertRequest,
    admin: AdminUser,
    session: SessionDep,
) -> AvailabilitySlot:
    return _service(session).upsert(data, actor_id=admin.id)
