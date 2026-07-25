"""Structured logging setup (structlog).

Every caught exception MUST be logged with context before it is handled —
silent ``except`` blocks are rejected in review. Use ``get_logger(__name__)``.
"""

import logging
import sys

import structlog

from app.core.config import settings


def configure_logging() -> None:
    """Configure structlog to emit JSON in prod, pretty console in dev."""
    logging.basicConfig(format="%(message)s", stream=sys.stdout, level=logging.INFO)

    processors: list = [
        structlog.contextvars.merge_contextvars,
        structlog.processors.add_log_level,
        structlog.processors.TimeStamper(fmt="iso"),
        structlog.processors.StackInfoRenderer(),
        structlog.processors.format_exc_info,
    ]
    if settings.is_production:
        processors.append(structlog.processors.JSONRenderer())
    else:
        processors.append(structlog.dev.ConsoleRenderer())

    structlog.configure(
        processors=processors,
        wrapper_class=structlog.make_filtering_bound_logger(logging.INFO),
        logger_factory=structlog.PrintLoggerFactory(),
        cache_logger_on_first_use=True,
    )


def get_logger(name: str) -> structlog.stdlib.BoundLogger:
    return structlog.get_logger(name)
