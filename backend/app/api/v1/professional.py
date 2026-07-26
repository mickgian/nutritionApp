"""Professional profile endpoint: the studio nutritionist card + reviews.

Any authenticated user may read it (not admin-gated); the professional and its
reviews are studio reference data, not per-user owned.
"""

from fastapi import APIRouter

from app.api.deps import CurrentUser, SessionDep
from app.repositories.professional_repository import ProfessionalRepository
from app.schemas.professional import ProfessionalRead
from app.services.professional_service import ProfessionalService

router = APIRouter(tags=["professional"])


@router.get("/professional", response_model=ProfessionalRead)
def get_professional(current_user: CurrentUser, session: SessionDep) -> ProfessionalRead:
    return ProfessionalService(ProfessionalRepository(session)).get_profile()
