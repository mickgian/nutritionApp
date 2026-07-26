"""Data access for :class:`Appointment`."""

from sqlmodel import Session, select

from app.models.appointment import Appointment


class AppointmentRepository:
    def __init__(self, session: Session) -> None:
        self.session = session

    def create(self, appointment: Appointment) -> Appointment:
        self.session.add(appointment)
        self.session.commit()
        self.session.refresh(appointment)
        return appointment

    def get_by_id(self, appointment_id: int) -> Appointment | None:
        return self.session.get(Appointment, appointment_id)

    def save(self, appointment: Appointment) -> Appointment:
        self.session.add(appointment)
        self.session.commit()
        self.session.refresh(appointment)
        return appointment

    def list_by_client(self, client_id: int) -> list[Appointment]:
        stmt = (
            select(Appointment)
            .where(Appointment.client_id == client_id)
            .order_by(Appointment.scheduled_at)
        )
        return list(self.session.exec(stmt).all())
