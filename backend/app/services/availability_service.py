"""Availability business logic — open/close slots and list a date range.

Studio-only (the router guards with ``require_admin``). Expected conditions
raise 4xx with Italian messages; never a 500.
"""

from datetime import UTC, date, datetime, time

import structlog
from fastapi import HTTPException, status

from app.models.availability import AvailabilitySlot
from app.repositories.availability_repository import AvailabilityRepository
from app.schemas.availability import AvailabilityUpsertRequest

logger = structlog.get_logger(__name__)


def _as_utc(dt: datetime) -> datetime:
    """Normalize to timezone-aware UTC (naive input is assumed to be UTC)."""
    if dt.tzinfo is None:
        return dt.replace(tzinfo=UTC)
    return dt.astimezone(UTC)


class AvailabilityService:
    def __init__(self, repo: AvailabilityRepository) -> None:
        self.repo = repo

    def upsert(self, data: AvailabilityUpsertRequest, actor_id: int) -> AvailabilitySlot:
        starts_at = _as_utc(data.starts_at)
        existing = self.repo.get_by_starts_at(starts_at)

        if existing is None:
            if starts_at <= datetime.now(UTC):
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail="Non puoi aprire disponibilità nel passato",
                )
            slot = AvailabilitySlot(
                starts_at=starts_at,
                duration_min=data.duration_min,
                is_open=data.is_open,
            )
            logger.info(
                "availability_created",
                operation="availability_upsert",
                user_id=actor_id,
                starts_at=starts_at.isoformat(),
            )
            return self.repo.save(slot)

        if existing.held_by_appointment_id is not None and not data.is_open:
            raise HTTPException(
                status_code=status.HTTP_409_CONFLICT,
                detail="Orario già prenotato, impossibile chiuderlo",
            )
        existing.is_open = data.is_open
        existing.duration_min = data.duration_min
        return self.repo.save(existing)

    def list_range(self, from_date: date, to_date: date) -> list[AvailabilitySlot]:
        start = datetime.combine(from_date, time.min, tzinfo=UTC)
        end = datetime.combine(to_date, time.max, tzinfo=UTC)
        return self.repo.list_between(start, end)
