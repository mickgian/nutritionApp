"""Health and readiness endpoints."""

from fastapi import APIRouter
from sqlalchemy import text

from app.api.deps import SessionDep

router = APIRouter()


@router.get("/health")
def health() -> dict[str, str]:
    """Liveness check — does the process respond?"""
    return {"status": "ok"}


@router.get("/ready")
def ready(session: SessionDep) -> dict[str, str]:
    """Readiness check — can we reach the database?"""
    session.connection().execute(text("SELECT 1"))
    return {"status": "ready"}
