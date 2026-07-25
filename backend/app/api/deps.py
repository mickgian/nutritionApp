"""Shared FastAPI dependencies: current user, role guards.

User-data isolation is enforced here: ``get_current_user`` resolves the caller
from the JWT, and ``require_admin`` guards studio-only endpoints. A ``cliente``
must only ever touch their own rows — repositories filter by the caller's id.
"""

from typing import Annotated

from fastapi import Depends, HTTPException, status
from fastapi.security import OAuth2PasswordBearer
from sqlmodel import Session

from app.core.database import get_session
from app.core.security import decode_token
from app.models.user import User, UserRole

oauth2_scheme = OAuth2PasswordBearer(tokenUrl="/api/v1/auth/login")

SessionDep = Annotated[Session, Depends(get_session)]
TokenDep = Annotated[str, Depends(oauth2_scheme)]


def get_current_user(session: SessionDep, token: TokenDep) -> User:
    claims = decode_token(token)
    if claims is None or "sub" not in claims:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Credenziali non valide",
            headers={"WWW-Authenticate": "Bearer"},
        )
    user = session.get(User, int(claims["sub"]))
    if user is None or not user.is_active:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Utente non trovato")
    return user


CurrentUser = Annotated[User, Depends(get_current_user)]


def require_admin(user: CurrentUser) -> User:
    if user.role != UserRole.admin:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Accesso riservato allo studio",
        )
    return user


AdminUser = Annotated[User, Depends(require_admin)]
