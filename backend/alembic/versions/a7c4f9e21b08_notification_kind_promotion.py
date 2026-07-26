"""add promotion to notificationkind enum

Revision ID: a7c4f9e21b08
Revises: 65ef700c31d0
Create Date: 2026-07-26 13:40:00.000000+00:00

DEV-080: the studio can broadcast a promotion, stored as a ``promotion``
notification. On PostgreSQL the native enum ``notificationkind`` must learn the
new value; on SQLite the column is a plain VARCHAR so nothing is required.
"""

from collections.abc import Sequence

from alembic import op

# revision identifiers, used by Alembic.
revision: str = "a7c4f9e21b08"
down_revision: str | None = "65ef700c31d0"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None

_ENUM_VALUES = "'appointment_reminder', 'order_deadline', 'box_ready', 'box_proposal'"


def upgrade() -> None:
    bind = op.get_bind()
    if bind.dialect.name == "postgresql":
        op.execute("ALTER TYPE notificationkind ADD VALUE IF NOT EXISTS 'promotion'")


def downgrade() -> None:
    bind = op.get_bind()
    if bind.dialect.name != "postgresql":
        return
    # PostgreSQL cannot drop a single enum value; recreate the type without it.
    op.execute("ALTER TYPE notificationkind RENAME TO notificationkind_old")
    op.execute(f"CREATE TYPE notificationkind AS ENUM ({_ENUM_VALUES})")
    op.execute(
        "ALTER TABLE notifications ALTER COLUMN kind TYPE notificationkind "
        "USING kind::text::notificationkind"
    )
    op.execute("DROP TYPE notificationkind_old")
