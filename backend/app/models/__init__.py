"""SQLModel table models.

Import every model here so Alembic autogenerate and ``SQLModel.metadata`` see
them. One concern per file.
"""

from app.models.appointment import Appointment  # noqa: F401
from app.models.availability import AvailabilitySlot  # noqa: F401
from app.models.professional import Professional  # noqa: F401
from app.models.review import Review  # noqa: F401
from app.models.user import User  # noqa: F401
