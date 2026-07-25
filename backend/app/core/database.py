"""Database engine and session management (SQLModel).

We use SQLModel exclusively — never SQLAlchemy ``declarative_base``. Models live
in ``app/models/`` and inherit from ``SQLModel`` with ``table=True``.

The engine is created lazily (on first use) so that importing the app does not
require a database driver to be installed — tests override ``get_session`` with
an in-memory SQLite engine and never touch PostgreSQL.
"""

from collections.abc import Generator
from functools import lru_cache

from sqlalchemy.engine import Engine
from sqlmodel import Session, create_engine

from app.core.config import settings


@lru_cache
def get_engine() -> Engine:
    return create_engine(
        settings.database_url,
        echo=settings.debug,
        pool_pre_ping=True,
    )


def get_session() -> Generator[Session, None, None]:
    """FastAPI dependency that yields a database session."""
    with Session(get_engine()) as session:
        yield session
