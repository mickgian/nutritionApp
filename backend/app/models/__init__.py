"""SQLModel table models.

Import every model here so Alembic autogenerate and ``SQLModel.metadata`` see
them. One concern per file.
"""

from app.models.user import User  # noqa: F401
from app.models.appointment import Appointment  # noqa: F401
