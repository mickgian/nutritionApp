"""meals, meal_boxes, box_items tables

Revision ID: f1ffbe932291
Revises: 88bf64de43e0
Create Date: 2026-07-26 08:58:44.244359+00:00

"""

from collections.abc import Sequence

import sqlalchemy as sa
import sqlmodel
from alembic import op

# revision identifiers, used by Alembic.
revision: str = "f1ffbe932291"
down_revision: str | None = "88bf64de43e0"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None

# MealSlot is shared by meals.slot and box_items.slot. On PostgreSQL a native
# ENUM type must be created exactly once; the per-column definitions therefore
# use create_type=False and we create/drop the type explicitly. On SQLite these
# are no-ops (enum columns are stored as text).
mealslot = sa.Enum("lunch", "dinner", name="mealslot")


def _slot_column() -> sa.Column:
    return sa.Column(
        "slot", sa.Enum("lunch", "dinner", name="mealslot", create_type=False), nullable=False
    )


def upgrade() -> None:
    mealslot.create(op.get_bind(), checkfirst=True)
    op.create_table(
        "meals",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("title", sqlmodel.sql.sqltypes.AutoString(length=160), nullable=False),
        _slot_column(),
        sa.Column("kcal", sa.Integer(), nullable=False),
        sa.Column("protein_g", sa.Integer(), nullable=False),
        sa.Column("carb_g", sa.Integer(), nullable=False),
        sa.Column("fat_g", sa.Integer(), nullable=False),
        sa.Column("asset_ref", sqlmodel.sql.sqltypes.AutoString(length=255), nullable=True),
        sa.Column("storage_note", sqlmodel.sql.sqltypes.AutoString(length=500), nullable=True),
        sa.Column("reheat_note", sqlmodel.sql.sqltypes.AutoString(length=500), nullable=True),
        sa.Column("created_at", sa.DateTime(), nullable=False),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(op.f("ix_meals_slot"), "meals", ["slot"], unique=False)
    op.create_table(
        "meal_boxes",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("client_id", sa.Integer(), nullable=False),
        sa.Column("plan_id", sa.Integer(), nullable=False),
        sa.Column("week_index", sa.Integer(), nullable=False),
        sa.Column("created_at", sa.DateTime(), nullable=False),
        sa.ForeignKeyConstraint(["client_id"], ["users.id"]),
        sa.ForeignKeyConstraint(["plan_id"], ["nutrition_plans.id"]),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(op.f("ix_meal_boxes_client_id"), "meal_boxes", ["client_id"], unique=False)
    op.create_index(op.f("ix_meal_boxes_week_index"), "meal_boxes", ["week_index"], unique=False)
    op.create_table(
        "box_items",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("box_id", sa.Integer(), nullable=False),
        sa.Column("meal_id", sa.Integer(), nullable=False),
        sa.Column("weekday", sa.Integer(), nullable=False),
        _slot_column(),
        sa.ForeignKeyConstraint(["box_id"], ["meal_boxes.id"]),
        sa.ForeignKeyConstraint(["meal_id"], ["meals.id"]),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("box_id", "weekday", "slot", name="uq_box_items_cell"),
    )
    op.create_index(op.f("ix_box_items_box_id"), "box_items", ["box_id"], unique=False)


def downgrade() -> None:
    op.drop_index(op.f("ix_box_items_box_id"), table_name="box_items")
    op.drop_table("box_items")
    op.drop_index(op.f("ix_meal_boxes_week_index"), table_name="meal_boxes")
    op.drop_index(op.f("ix_meal_boxes_client_id"), table_name="meal_boxes")
    op.drop_table("meal_boxes")
    op.drop_index(op.f("ix_meals_slot"), table_name="meals")
    op.drop_table("meals")
    # Drop the shared enum type on PostgreSQL (no-op on SQLite).
    bind = op.get_bind()
    if bind.dialect.name == "postgresql":
        sa.Enum(name="mealslot").drop(bind, checkfirst=True)
