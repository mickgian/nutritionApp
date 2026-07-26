"""Weekly meal box models — the 14-meal menu for a client's plan week.

``MealBox`` is owned per-client (Rule 4: every query filters by ``client_id``)
and tied to a nutrition plan + week index. ``BoxItem`` joins a catalog meal to a
(weekday, slot) cell of that box; a box holds exactly 14 items (7 days x
lunch/dinner), enforced by the unique index on (box_id, weekday, slot).
"""

from datetime import UTC, datetime

from sqlmodel import Field, SQLModel, UniqueConstraint

from app.models.meal import MealSlot


class MealBox(SQLModel, table=True):
    __tablename__ = "meal_boxes"

    id: int | None = Field(default=None, primary_key=True)
    # Ownership: every query MUST filter by client_id for a non-admin caller.
    client_id: int = Field(foreign_key="users.id", index=True)
    plan_id: int = Field(foreign_key="nutrition_plans.id")
    week_index: int = Field(index=True)
    created_at: datetime = Field(default_factory=lambda: datetime.now(UTC))


class BoxItem(SQLModel, table=True):
    __tablename__ = "box_items"
    __table_args__ = (UniqueConstraint("box_id", "weekday", "slot", name="uq_box_items_cell"),)

    id: int | None = Field(default=None, primary_key=True)
    box_id: int = Field(foreign_key="meal_boxes.id", index=True)
    meal_id: int = Field(foreign_key="meals.id")
    weekday: int  # 0-6 (Mon..Sun)
    slot: MealSlot
