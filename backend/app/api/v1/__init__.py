"""API v1 router aggregation."""

from fastapi import APIRouter

from app.api.v1 import (
    appointments,
    auth,
    availability,
    box,
    credit,
    health,
    meals,
    notifications,
    orders,
    plans,
    professional,
)

api_router = APIRouter()
api_router.include_router(auth.router)
api_router.include_router(availability.router)
api_router.include_router(appointments.router)
api_router.include_router(professional.router)
api_router.include_router(plans.router)
api_router.include_router(box.router)
api_router.include_router(meals.router)
api_router.include_router(orders.router)
api_router.include_router(credit.router)
api_router.include_router(notifications.router)
api_router.include_router(health.router, tags=["health"])
