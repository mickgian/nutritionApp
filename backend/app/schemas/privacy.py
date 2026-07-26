"""Schemas for the GDPR personal-data export (DEV-093).

Deliberately slim, export-only read models — they never include secrets such as
a payment ``provider_token`` or the account's ``hashed_password``.
"""

from datetime import datetime

from pydantic import BaseModel


class AccountExport(BaseModel):
    id: int
    email: str
    full_name: str
    role: str
    is_active: bool
    created_at: datetime


class AppointmentExport(BaseModel):
    id: int
    visit_type: str
    scheduled_at: datetime
    status: str
    price_cents: int
    created_at: datetime


class OrderExport(BaseModel):
    id: int
    plan_id: int
    formula: str
    pickup: str
    status: str
    price_cents: int
    created_at: datetime


class PaymentExport(BaseModel):
    id: int
    appointment_id: int | None
    order_id: int | None
    amount_cents: int
    method: str
    provider: str
    status: str
    created_at: datetime


class PlanExport(BaseModel):
    id: int
    name: str
    daily_kcal: int
    weeks: int
    is_active: bool
    created_at: datetime


class BoxExport(BaseModel):
    id: int
    plan_id: int
    week_index: int
    created_at: datetime


class CreditExport(BaseModel):
    id: int
    amount_cents: int
    reason: str
    is_used: bool
    created_at: datetime
    expires_at: datetime


class NotificationExport(BaseModel):
    id: int
    kind: str
    title: str
    body: str
    is_read: bool
    created_at: datetime


class ReviewExport(BaseModel):
    id: int
    professional_id: int
    rating: int
    body: str
    created_at: datetime


class UserDataExport(BaseModel):
    """Everything Meridia holds about the caller, in one portable document."""

    generated_at: datetime
    account: AccountExport
    appointments: list[AppointmentExport]
    orders: list[OrderExport]
    payments: list[PaymentExport]
    nutrition_plans: list[PlanExport]
    meal_boxes: list[BoxExport]
    credits: list[CreditExport]
    notifications: list[NotificationExport]
    reviews: list[ReviewExport]
