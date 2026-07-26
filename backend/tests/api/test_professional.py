"""Tests for the professional profile & reviews endpoint (DEV-023).

The Consulenza card reads ``GET /api/v1/professional``: the studio nutritionist
(Dott.ssa Serra, seeded on first read) plus her reviews. Any authenticated user
may read it (it is not admin-gated); the read is idempotent (no duplicate seed);
and the read model renders gracefully with zero reviews.
"""

from fastapi.testclient import TestClient
from sqlmodel import Session

from app.models.professional import Professional
from app.models.review import Review
from app.repositories.professional_repository import ProfessionalRepository
from app.schemas.professional import ProfessionalRead

PROFESSIONAL = "/api/v1/professional"


def test_requires_authentication(client: TestClient):
    resp = client.get(PROFESSIONAL)
    assert resp.status_code == 401


def test_cliente_can_read_seeded_profile(client: TestClient, cliente_headers):
    resp = client.get(PROFESSIONAL, headers=cliente_headers)
    assert resp.status_code == 200
    body = resp.json()
    assert body["full_name"] == "Dott.ssa Anna Serra"
    assert body["title"] == "Biologa Nutrizionista"
    assert body["bio"]
    assert body["reviews_count"] == len(body["reviews"])
    assert body["reviews_count"] > 0
    assert body["rating"] > 0
    # A review carries a display name, a 1-5 rating, and Italian body text.
    first = body["reviews"][0]
    assert first["author_name"]
    assert 1 <= first["rating"] <= 5
    assert first["body"]


def test_admin_can_read_profile(client: TestClient, admin_headers):
    resp = client.get(PROFESSIONAL, headers=admin_headers)
    assert resp.status_code == 200


def test_read_is_idempotent_no_duplicate_seed(
    client: TestClient, session: Session, cliente_headers
):
    first = client.get(PROFESSIONAL, headers=cliente_headers).json()
    second = client.get(PROFESSIONAL, headers=cliente_headers).json()
    assert first["id"] == second["id"]
    assert first["reviews_count"] == second["reviews_count"]
    # Exactly one professional row and one seeded review set exist.
    assert len(ProfessionalRepository(session).all_professionals()) == 1


def test_rating_avg_reflects_seeded_reviews(client: TestClient, session: Session, cliente_headers):
    body = client.get(PROFESSIONAL, headers=cliente_headers).json()
    assert session.get(Professional, body["id"]) is not None
    stored = ProfessionalRepository(session).list_reviews(body["id"])
    expected_avg = round(sum(r.rating for r in stored) / len(stored), 2)
    assert body["rating"] == expected_avg
    assert body["reviews_count"] == len(stored)


def test_read_model_handles_empty_reviews():
    professional = Professional(
        id=1,
        full_name="Dott.ssa Anna Serra",
        title="Biologa Nutrizionista",
        bio="Bio.",
        rating_avg=0,
        reviews_count=0,
    )
    read = ProfessionalRead.build(professional, [])
    assert read.reviews == []
    assert read.rating == 0.0
    assert read.reviews_count == 0


def test_read_model_computes_rating_from_avg():
    professional = Professional(
        id=1,
        full_name="X",
        title="Y",
        bio="Z",
        rating_avg=487,
        reviews_count=2,
    )
    reviews = [
        Review(id=1, professional_id=1, author_name="Giulia M.", rating=5, body="Ottimo"),
        Review(id=2, professional_id=1, author_name="Marco T.", rating=4, body="Bene"),
    ]
    read = ProfessionalRead.build(professional, reviews)
    assert read.rating == 4.87
    assert len(read.reviews) == 2
    assert read.reviews[0].author_name == "Giulia M."
