"""Client credit endpoint: list the caller's own active store credits.

``GET /me/credits`` returns only the caller's credits (Rule 4 — isolation),
excluding used or expired ones (DEV-050).
"""

from fastapi import APIRouter

from app.api.deps import CurrentUser, SessionDep
from app.models.credit import Credit
from app.repositories.credit_repository import CreditRepository
from app.schemas.credit import CreditRead
from app.services.credit_service import CreditService

router = APIRouter(tags=["credits"])


@router.get("/me/credits", response_model=list[CreditRead])
def list_my_credits(current_user: CurrentUser, session: SessionDep) -> list[Credit]:
    return CreditService(CreditRepository(session)).list_active(current_user.id)
