"""Unit tests for password hashing, JWT helpers, and security config guards."""

from datetime import UTC, datetime, timedelta

import pytest
from jose import jwt

from app.core.config import Settings, settings
from app.core.security import (
    create_access_token,
    decode_token,
    hash_password,
    verify_password,
)


def test_password_round_trip():
    hashed = hash_password("s3greto!")
    assert hashed != "s3greto!"
    assert verify_password("s3greto!", hashed)
    assert not verify_password("sbagliato", hashed)


def test_jwt_round_trip():
    token = create_access_token(subject="42", role="cliente")
    claims = decode_token(token)
    assert claims is not None
    assert claims["sub"] == "42"
    assert claims["role"] == "cliente"


def test_decode_invalid_token_returns_none():
    assert decode_token("not-a-jwt") is None


def test_decode_rejects_token_without_expiry():
    # A token with no ``exp`` never expires — reject it rather than trust it.
    token = jwt.encode(
        {"sub": "1", "role": "cliente"},
        settings.secret_key,
        algorithm=settings.jwt_algorithm,
    )
    assert decode_token(token) is None


def test_decode_rejects_token_without_subject():
    token = jwt.encode(
        {"role": "cliente", "exp": datetime.now(UTC) + timedelta(minutes=5)},
        settings.secret_key,
        algorithm=settings.jwt_algorithm,
    )
    assert decode_token(token) is None


def test_decode_rejects_expired_token():
    token = jwt.encode(
        {"sub": "1", "role": "cliente", "exp": datetime.now(UTC) - timedelta(minutes=1)},
        settings.secret_key,
        algorithm=settings.jwt_algorithm,
    )
    assert decode_token(token) is None


def test_production_rejects_default_secret():
    with pytest.raises(ValueError):
        Settings(environment="production", secret_key="change-me-in-production")


def test_production_rejects_short_secret():
    with pytest.raises(ValueError):
        Settings(environment="production", secret_key="too-short")


def test_production_accepts_strong_secret():
    strong = "x" * 48
    assert Settings(environment="production", secret_key=strong).is_production


def test_development_allows_default_secret():
    # The placeholder is fine for local dev; the guard only bites in production.
    assert Settings(environment="development", secret_key="change-me-in-production")
