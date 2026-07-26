"""users: add privacy_consent_at

Revision ID: c1a2b3d4e5f6
Revises: a7c4f9e21b08
Create Date: 2026-07-26 15:10:00.000000+00:00

DEV-095: record explicit privacy-policy consent. Nullable timestamptz — existing
rows keep NULL (consent was implicit before this column existed); new
registrations set it to the moment consent was given.
"""

from collections.abc import Sequence

import sqlalchemy as sa
from alembic import op

# revision identifiers, used by Alembic.
revision: str = "c1a2b3d4e5f6"
down_revision: str | None = "a7c4f9e21b08"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    op.add_column(
        "users",
        sa.Column("privacy_consent_at", sa.DateTime(timezone=True), nullable=True),
    )


def downgrade() -> None:
    op.drop_column("users", "privacy_consent_at")
