"""Unit tests for password hashing and JWT helpers."""

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
