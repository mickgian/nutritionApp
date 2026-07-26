"""Authentication endpoints: register, login, me."""

from fastapi import APIRouter, status

from app.api.deps import CurrentUser, SessionDep
from app.repositories.user_repository import UserRepository
from app.schemas.auth import LoginRequest, RegisterRequest, TokenResponse, UserResponse
from app.services.auth_service import AuthService

router = APIRouter(prefix="/auth", tags=["auth"])


def _service(session: SessionDep) -> AuthService:
    return AuthService(UserRepository(session))


@router.post("/register", response_model=UserResponse, status_code=status.HTTP_201_CREATED)
def register(data: RegisterRequest, session: SessionDep) -> UserResponse:
    user = _service(session).register(data)
    return UserResponse.model_validate(user)


@router.post("/login", response_model=TokenResponse)
def login(data: LoginRequest, session: SessionDep) -> TokenResponse:
    service = _service(session)
    user = service.authenticate(data)
    return TokenResponse(access_token=service.issue_token(user))


@router.get("/me", response_model=UserResponse)
def me(current_user: CurrentUser) -> UserResponse:
    return UserResponse.model_validate(current_user)
