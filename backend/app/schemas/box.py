"""Schemas for the weekly meal box & meal catalog (Pydantic v2)."""

from pydantic import BaseModel, ConfigDict

from app.models.meal import MealSlot


class MealRead(BaseModel):
    """A catalog meal with its nutrition and handling notes."""

    model_config = ConfigDict(from_attributes=True)

    id: int
    title: str
    slot: MealSlot
    kcal: int
    protein_g: int
    carb_g: int
    fat_g: int
    asset_ref: str | None = None
    storage_note: str | None = None
    reheat_note: str | None = None


class BoxItemRead(BaseModel):
    """One (weekday, slot) cell of the weekly box with its meal."""

    weekday: int
    slot: MealSlot
    meal: MealRead


class BoxRead(BaseModel):
    week_index: int
    items: list[BoxItemRead]
