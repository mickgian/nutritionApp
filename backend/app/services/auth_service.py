"""Authentication business logic: registration, credential checks, token issue.

Raises 4xx HTTPExceptions with Italian messages for expected conditions — never
a 500. Every branch is covered by tests/api/test_auth.py.
"""

import structlog
from fastapi import HTTPException, status

from app.core.security import create_access_token, hash_password, verify_password
from app.models.user import User, UserRole
from app.repositories.user_repository import UserRepository, normalize_email
from app.schemas.auth import LoginRequest, RegisterRequest

logger = structlog.get_logger(__name__)


class AuthService:
    def __init__(self, repo: UserRepository) -> None:
        self.repo = repo

    def register(self, data: RegisterRequest) -> User:
        if self.repo.get_by_email(data.email) is not None:
            raise HTTPException(
                status_code=status.HTTP_409_CONFLICT,
                detail="Email già registrata",
            )
        user = User(
            email=normalize_email(data.email),
            hashed_password=hash_password(data.password),
            full_name=data.full_name.strip(),
            role=UserRole.cliente,
        )
        created = self.repo.create(user)
        logger.info("user_registered", user_id=created.id, operation="register")
        return created

    def authenticate(self, data: LoginRequest) -> User:
        user = self.repo.get_by_email(data.email)
        if user is None or not verify_password(data.password, user.hashed_password):
            logger.info("login_failed", operation="login", error_type="invalid_credentials")
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Credenziali non valide",
                headers={"WWW-Authenticate": "Bearer"},
            )
        if not user.is_active:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="Account disattivato",
            )
        return user

    def issue_token(self, user: User) -> str:
        return create_access_token(subject=str(user.id), role=user.role.value)
