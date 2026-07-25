"""Data access for :class:`AvailabilitySlot`."""

from datetime import datetime

from sqlmodel import Session, select

from app.models.availability import AvailabilitySlot


class AvailabilityRepository:
    def __init__(self, session: Session) -> None:
        self.session = session

    def get_by_id(self, slot_id: int) -> AvailabilitySlot | None:
        return self.session.get(AvailabilitySlot, slot_id)

    def get_by_starts_at(self, starts_at: datetime) -> AvailabilitySlot | None:
        stmt = select(AvailabilitySlot).where(AvailabilitySlot.starts_at == starts_at)
        return self.session.exec(stmt).first()

    def list_between(self, start: datetime, end: datetime) -> list[AvailabilitySlot]:
        stmt = (
            select(AvailabilitySlot)
            .where(AvailabilitySlot.starts_at >= start, AvailabilitySlot.starts_at <= end)
            .order_by(AvailabilitySlot.starts_at)
        )
        return list(self.session.exec(stmt).all())

    def save(self, slot: AvailabilitySlot) -> AvailabilitySlot:
        self.session.add(slot)
        self.session.commit()
        self.session.refresh(slot)
        return slot
