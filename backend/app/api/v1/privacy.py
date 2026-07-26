"""GDPR endpoints (DEV-093): personal-data export and account erasure.

Both act only on the caller (``CurrentUser``) — a client can export or erase
their own data and no one else's (Rule 4). See ADR-003 in
``app.services.privacy_service`` for the retention/erasure rationale.
"""

from fastapi import APIRouter, status

from app.api.deps import CurrentUser, SessionDep
from app.schemas.privacy import UserDataExport
from app.services.privacy_service import PrivacyService

router = APIRouter(tags=["privacy"])


@router.get("/me/export", response_model=UserDataExport)
def export_my_data(current_user: CurrentUser, session: SessionDep) -> UserDataExport:
    """Right to data portability (GDPR Art. 15/20) — export all of my data."""
    return PrivacyService(session).export(current_user)


@router.delete("/me", status_code=status.HTTP_204_NO_CONTENT)
def delete_my_account(current_user: CurrentUser, session: SessionDep) -> None:
    """Right to erasure (GDPR Art. 17) — delete my data, anonymise my account."""
    PrivacyService(session).delete_account(current_user)
