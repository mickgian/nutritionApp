"""appointments: add slot_id, price_eur -> price_cents

Revision ID: 99d2ac242600
Revises: 206417ece486
Create Date: 2026-07-25 22:40:24.176967+00:00

"""

from collections.abc import Sequence

import sqlalchemy as sa
from alembic import op

# revision identifiers, used by Alembic.
revision: str = "99d2ac242600"
down_revision: str | None = "206417ece486"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    # Add slot_id (FK -> availability_slots) and the new cents column.
    with op.batch_alter_table("appointments", schema=None) as batch_op:
        batch_op.add_column(sa.Column("slot_id", sa.Integer(), nullable=True))
        batch_op.add_column(sa.Column("price_cents", sa.Integer(), nullable=True))
        batch_op.create_index(batch_op.f("ix_appointments_slot_id"), ["slot_id"], unique=False)
        batch_op.create_foreign_key(
            "fk_appointments_slot_id_availability_slots",
            "availability_slots",
            ["slot_id"],
            ["id"],
        )
    # Migrate existing whole-euro prices to integer cents.
    op.execute("UPDATE appointments SET price_cents = price_eur * 100")
    with op.batch_alter_table("appointments", schema=None) as batch_op:
        batch_op.alter_column("price_cents", existing_type=sa.Integer(), nullable=False)
        batch_op.drop_column("price_eur")


def downgrade() -> None:
    with op.batch_alter_table("appointments", schema=None) as batch_op:
        batch_op.add_column(sa.Column("price_eur", sa.Integer(), nullable=True))
    op.execute("UPDATE appointments SET price_eur = price_cents / 100")
    with op.batch_alter_table("appointments", schema=None) as batch_op:
        batch_op.alter_column("price_eur", existing_type=sa.Integer(), nullable=False)
        batch_op.drop_index(batch_op.f("ix_appointments_slot_id"))
        batch_op.drop_column("slot_id")
        batch_op.drop_column("price_cents")
