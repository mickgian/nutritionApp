"""Pytest fixtures — in-memory SQLite DB and a FastAPI TestClient.

Tests run against an isolated in-memory database so they need no PostgreSQL.
Integration tests that exercise Postgres-specific behavior can override this.
"""

import pytest
from fastapi.testclient import TestClient
from sqlmodel import Session, SQLModel, create_engine
from sqlmodel.pool import StaticPool

import app.models
from app.api.deps import get_session
from app.core.security import create_access_token, hash_password
from app.main import app
from app.models.user import User, UserRole


@pytest.fixture(name="session")
def session_fixture():
    engine = create_engine(
        "sqlite://",
        connect_args={"check_same_thread": False},
        poolclass=StaticPool,
    )
    SQLModel.metadata.create_all(engine)
    with Session(engine) as session:
        yield session


@pytest.fixture(name="client")
def client_fixture(session: Session):
    def _get_session_override():
        return session

    app.dependency_overrides[get_session] = _get_session_override
    yield TestClient(app)
    app.dependency_overrides.clear()


@pytest.fixture(name="make_user")
def make_user_fixture(session: Session):
    """Factory that inserts a user (default: a cliente) and returns it."""

    def _make(
        email: str,
        role: UserRole = UserRole.cliente,
        password: str = "password123",
    ) -> User:
        user = User(
            email=email,
            hashed_password=hash_password(password),
            full_name="Test User",
            role=role,
        )
        session.add(user)
        session.commit()
        session.refresh(user)
        return user

    return _make


def _auth_header(user: User) -> dict[str, str]:
    token = create_access_token(subject=str(user.id), role=user.role.value)
    return {"Authorization": f"Bearer {token}"}


@pytest.fixture(name="admin_headers")
def admin_headers_fixture(make_user) -> dict[str, str]:
    return _auth_header(make_user("admin@studiomeridia.it", UserRole.admin))


@pytest.fixture(name="cliente_headers")
def cliente_headers_fixture(make_user) -> dict[str, str]:
    return _auth_header(make_user("cliente@example.com", UserRole.cliente))
