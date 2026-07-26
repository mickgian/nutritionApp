"""Meal model — the studio's reference catalog of prepared meals.

Shared menu data (not per-user owned): each meal carries its kcal + macros and
conservazione/riscaldamento notes (shown in the meal detail, DEV-032). A weekly
box references meals via ``box_items`` (DEV-031).
"""

from datetime import UTC, datetime
from enum import Enum

from sqlmodel import Field, SQLModel


class MealSlot(str, Enum):
    lunch = "lunch"
    dinner = "dinner"


class Meal(SQLModel, table=True):
    __tablename__ = "meals"

    id: int | None = Field(default=None, primary_key=True)
    title: str = Field(max_length=160)  # Italian
    slot: MealSlot = Field(index=True)
    kcal: int
    protein_g: int
    carb_g: int
    fat_g: int
    asset_ref: str | None = Field(default=None, max_length=255)  # emoji/image key
    storage_note: str | None = Field(default=None, max_length=500)  # conservazione
    reheat_note: str | None = Field(default=None, max_length=500)  # riscaldamento
    created_at: datetime = Field(default_factory=lambda: datetime.now(UTC))
