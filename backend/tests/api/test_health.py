"""Smoke tests for the health endpoints."""

from fastapi.testclient import TestClient


def test_health_ok(client: TestClient):
    resp = client.get("/api/v1/health")
    assert resp.status_code == 200
    assert resp.json() == {"status": "ok"}


def test_ready_reaches_db(client: TestClient):
    resp = client.get("/api/v1/ready")
    assert resp.status_code == 200
    assert resp.json() == {"status": "ready"}


def test_root(client: TestClient):
    resp = client.get("/")
    assert resp.status_code == 200
    assert resp.json()["docs"] == "/docs"
