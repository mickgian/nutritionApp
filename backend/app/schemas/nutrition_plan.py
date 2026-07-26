"""Schemas for nutrition plan assignment & the client's plan view (Pydantic v2)."""

from datetime import datetime

from pydantic import BaseModel, ConfigDict, Field


class PlanAssignRequest(BaseModel):
    """Studio assigns a plan to a client (admin-only)."""

    name: str = Field(min_length=1, max_length=120)
    daily_kcal: int = Field(ge=1, le=10000)
    weeks: int = Field(ge=1, le=52)


class PlanRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    name: str
    daily_kcal: int
    weeks: int
    is_active: bool
    created_at: datetime
