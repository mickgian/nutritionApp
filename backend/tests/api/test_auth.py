"""Tests for the authentication endpoints (register / login / me).

Covers the happy paths plus the edges the rubric requires: duplicate email,
bad credentials, weak password, invalid email, and token-guarded access.
"""

from fastapi.testclient import TestClient
from sqlmodel import Session, select

from app.core.security import create_access_token, decode_token
from app.models.user import User, UserRole

REGISTER = "/api/v1/auth/register"
LOGIN = "/api/v1/auth/login"
ME = "/api/v1/auth/me"


def _register(
    client: TestClient, email="mario@example.com", password="password123", name="Mario Rossi"
):
    return client.post(REGISTER, json={"email": email, "password": password, "full_name": name})


# ----------------------------- register ----------------------------------- #


def test_register_returns_201_and_public_user(client: TestClient):
    resp = _register(client)
    assert resp.status_code == 201
    body = resp.json()
    assert body["email"] == "mario@example.com"
    assert body["full_name"] == "Mario Rossi"
    assert body["role"] == UserRole.cliente.value
    assert "id" in body
    # the password (hashed or plain) must never leak in the response
    assert "password" not in body
    assert "hashed_password" not in body


def test_register_never_stores_plaintext_password(client: TestClient, session: Session):
    _register(client)
    stored = session.exec(select(User).where(User.email == "mario@example.com")).one()
    assert stored.hashed_password != "password123"
    assert stored.hashed_password  # something was hashed


def test_register_duplicate_email_returns_409_italian(client: TestClient):
    _register(client)
    resp = _register(client)
    assert resp.status_code == 409
    assert resp.json()["detail"] == "Email già registrata"


def test_register_duplicate_email_is_case_insensitive(client: TestClient):
    _register(client, email="Mario@Example.com")
    resp = _register(client, email="mario@example.com")
    assert resp.status_code == 409


def test_register_weak_password_returns_422(client: TestClient):
    resp = _register(client, password="short")
    assert resp.status_code == 422


def test_register_invalid_email_returns_422(client: TestClient):
    resp = _register(client, email="not-an-email")
    assert resp.status_code == 422


def test_register_blank_name_returns_422(client: TestClient):
    resp = _register(client, name="")
    assert resp.status_code == 422


# ------------------------------- login ------------------------------------ #


def test_login_returns_valid_bearer_token(client: TestClient):
    _register(client)
    resp = client.post(LOGIN, json={"email": "mario@example.com", "password": "password123"})
    assert resp.status_code == 200
    body = resp.json()
    assert body["token_type"] == "bearer"
    claims = decode_token(body["access_token"])
    assert claims is not None
    assert claims["role"] == UserRole.cliente.value


def test_login_is_case_insensitive_on_email(client: TestClient):
    _register(client, email="mario@example.com")
    resp = client.post(LOGIN, json={"email": "MARIO@example.com", "password": "password123"})
    assert resp.status_code == 200


def test_login_bad_password_returns_401_italian(client: TestClient):
    _register(client)
    resp = client.post(LOGIN, json={"email": "mario@example.com", "password": "wrongpass1"})
    assert resp.status_code == 401
    assert resp.json()["detail"] == "Credenziali non valide"


def test_login_unknown_email_returns_401(client: TestClient):
    resp = client.post(LOGIN, json={"email": "ghost@example.com", "password": "password123"})
    assert resp.status_code == 401


def test_login_inactive_account_returns_403(client: TestClient, session: Session):
    _register(client)
    user = session.exec(select(User).where(User.email == "mario@example.com")).one()
    user.is_active = False
    session.add(user)
    session.commit()
    resp = client.post(LOGIN, json={"email": "mario@example.com", "password": "password123"})
    assert resp.status_code == 403
    assert resp.json()["detail"] == "Account disattivato"


# -------------------------------- me -------------------------------------- #


def test_me_requires_token(client: TestClient):
    resp = client.get(ME)
    assert resp.status_code == 401


def test_me_rejects_invalid_token(client: TestClient):
    resp = client.get(ME, headers={"Authorization": "Bearer not-a-real-token"})
    assert resp.status_code == 401


def test_me_rejects_token_with_non_numeric_subject(client: TestClient):
    # A validly-signed token whose `sub` is not an int must 401, never 500.
    bad = create_access_token(subject="not-a-number", role=UserRole.cliente.value)
    resp = client.get(ME, headers={"Authorization": f"Bearer {bad}"})
    assert resp.status_code == 401


def test_me_with_token_for_missing_user_returns_401(client: TestClient):
    token = create_access_token(subject="99999", role=UserRole.cliente.value)
    resp = client.get(ME, headers={"Authorization": f"Bearer {token}"})
    assert resp.status_code == 401
    assert resp.json()["detail"] == "Utente non trovato"


def test_me_returns_current_user(client: TestClient):
    _register(client)
    token = client.post(
        LOGIN, json={"email": "mario@example.com", "password": "password123"}
    ).json()["access_token"]
    resp = client.get(ME, headers={"Authorization": f"Bearer {token}"})
    assert resp.status_code == 200
    body = resp.json()
    assert body["email"] == "mario@example.com"
    assert body["role"] == UserRole.cliente.value
