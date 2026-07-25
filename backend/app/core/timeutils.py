"""Small datetime helpers. All domain datetimes are UTC (Rule 7)."""

from datetime import UTC, datetime


def to_utc(dt: datetime) -> datetime:
    """Normalize to timezone-aware UTC (naive input is assumed to already be UTC)."""
    if dt.tzinfo is None:
        return dt.replace(tzinfo=UTC)
    return dt.astimezone(UTC)
