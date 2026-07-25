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
from app.main import app


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
