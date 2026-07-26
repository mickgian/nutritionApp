"""User model — a client (cliente) or the studio admin/nutritionist."""

from datetime import UTC, datetime
from enum import Enum

from sqlmodel import Field, SQLModel


class UserRole(str, Enum):
    cliente = "cliente"
    admin = "admin"


class User(SQLModel, table=True):
    __tablename__ = "users"

    id: int | None = Field(default=None, primary_key=True)
    email: str = Field(index=True, unique=True, max_length=255)
    hashed_password: str = Field(max_length=255)
    full_name: str = Field(max_length=120)
    role: UserRole = Field(default=UserRole.cliente, index=True)
    is_active: bool = Field(default=True)
    created_at: datetime = Field(default_factory=lambda: datetime.now(UTC))
    # GDPR: when the user accepted the privacy policy. NULL for accounts created
    # before explicit consent was recorded (consent was implicit at registration).
    privacy_consent_at: datetime | None = Field(default=None)
