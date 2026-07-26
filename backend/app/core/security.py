"""Password hashing and JWT token helpers.

Auth model: JWT access tokens (HS256). Passwords hashed with bcrypt via passlib.
Roles: ``cliente`` (default) and ``admin``. Never store plaintext passwords.
"""

from datetime import UTC, datetime, timedelta
from typing import Any

from jose import JWTError, jwt
from passlib.context import CryptContext

from app.core.config import settings

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")


def hash_password(plain: str) -> str:
    return pwd_context.hash(plain)


def verify_password(plain: str, hashed: str) -> bool:
    return pwd_context.verify(plain, hashed)


def create_access_token(subject: str, role: str, expires_minutes: int | None = None) -> str:
    """Create a signed JWT for the given subject (user id) and role."""
    expire = datetime.now(UTC) + timedelta(
        minutes=expires_minutes or settings.access_token_expire_minutes
    )
    payload: dict[str, Any] = {"sub": subject, "role": role, "exp": expire}
    return jwt.encode(payload, settings.secret_key, algorithm=settings.jwt_algorithm)


def decode_token(token: str) -> dict[str, Any] | None:
    """Decode and validate a JWT. Returns the claims or ``None`` if invalid.

    Requires both ``exp`` and ``sub``: a token missing an expiry (a
    never-expiring token) or a subject is rejected rather than trusted.
    """
    try:
        return jwt.decode(
            token,
            settings.secret_key,
            algorithms=[settings.jwt_algorithm],
            options={"require_exp": True, "require_sub": True},
        )
    except JWTError:
        return None
