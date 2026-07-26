"""Tests for the meal detail endpoint (DEV-032).

A client opens a meal from the box: ``GET /api/v1/meals/{id}`` returns the meal
with kcal + macros and conservazione/riscaldamento notes. Unknown id → 404
(Italian). Requires authentication.
"""

from fastapi.testclient import TestClient
from sqlmodel import Session

from app.models.meal import Meal, MealSlot


def _seed_meal(session: Session) -> Meal:
    meal = Meal(
        title="Pollo alle erbe con quinoa",
        slot=MealSlot.lunch,
        kcal=520,
        protein_g=42,
        carb_g=48,
        fat_g=16,
        asset_ref="🍗",
        storage_note="Conservare in frigorifero e consumare entro 3 giorni.",
        reheat_note="Riscaldare in microonde per 2-3 minuti.",
    )
    session.add(meal)
    session.commit()
    session.refresh(meal)
    return meal


def test_requires_authentication(client: TestClient, session: Session):
    meal = _seed_meal(session)
    resp = client.get(f"/api/v1/meals/{meal.id}")
    assert resp.status_code == 401


def test_get_meal_detail(client: TestClient, session: Session, cliente_headers):
    meal = _seed_meal(session)
    resp = client.get(f"/api/v1/meals/{meal.id}", headers=cliente_headers)
    assert resp.status_code == 200
    body = resp.json()
    assert body["title"] == "Pollo alle erbe con quinoa"
    assert body["kcal"] == 520
    assert body["protein_g"] == 42
    assert body["carb_g"] == 48
    assert body["fat_g"] == 16
    assert body["slot"] == "lunch"
    assert body["storage_note"]
    assert body["reheat_note"]


def test_unknown_meal_returns_404(client: TestClient, cliente_headers):
    resp = client.get("/api/v1/meals/9999", headers=cliente_headers)
    assert resp.status_code == 404
    assert resp.json()["detail"]
