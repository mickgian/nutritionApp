"""NutritionPlan model — a plan assigned by the studio to a client.

Assigning a plan unlocks the meal box (persona ``new`` = no active plan → the
client sees a locked box, DEV-033). Owned per-client (Rule 4): every query
filters by ``client_id``. At most one plan is active per client at a time.
"""

from datetime import UTC, datetime

from sqlmodel import Field, SQLModel


class NutritionPlan(SQLModel, table=True):
    __tablename__ = "nutrition_plans"

    id: int | None = Field(default=None, primary_key=True)
    # Ownership: every query MUST filter by client_id for a non-admin caller.
    client_id: int = Field(foreign_key="users.id", index=True)
    name: str = Field(max_length=120)  # e.g. "Dimagrimento 1400 kcal"
    daily_kcal: int
    weeks: int
    is_active: bool = Field(default=True, index=True)
    assigned_by: int | None = Field(default=None, foreign_key="users.id")
    created_at: datetime = Field(default_factory=lambda: datetime.now(UTC))
