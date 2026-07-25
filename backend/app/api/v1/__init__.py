"""API v1 router aggregation."""

from fastapi import APIRouter

from app.api.v1 import auth, availability, health

api_router = APIRouter()
api_router.include_router(auth.router)
api_router.include_router(availability.router)
api_router.include_router(health.router, tags=["health"])
