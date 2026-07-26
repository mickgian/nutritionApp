"""Tests for nutrition plan assignment (admin) + client view (DEV-030).

The studio assigns a plan to a client (``POST /api/v1/admin/clients/{id}/plan``,
admin-only); the client reads their own active plan (``GET /api/v1/me/plan``).
No active plan → 404 so the client renders the locked box (not an error). Covers
the admin gate, ownership isolation, the one-active-plan rule, and 404/422 edges.
"""

from fastapi.testclient import TestClient
from sqlmodel import Session

from app.models.user import UserRole

ME_PLAN = "/api/v1/me/plan"


def _admin_assign_path(client_id: int) -> str:
    return f"/api/v1/admin/clients/{client_id}/plan"


def _plan_body(name: str = "Dimagrimento 1400 kcal", kcal: int = 1400, weeks: int = 4) -> dict:
    return {"name": name, "daily_kcal": kcal, "weeks": weeks}


def test_admin_assigns_plan(client: TestClient, make_user, admin_headers):
    cliente = make_user("mario@example.com", UserRole.cliente)
    resp = client.post(_admin_assign_path(cliente.id), json=_plan_body(), headers=admin_headers)
    assert resp.status_code == 201
    body = resp.json()
    assert body["name"] == "Dimagrimento 1400 kcal"
    assert body["daily_kcal"] == 1400
    assert body["weeks"] == 4
    assert body["is_active"] is True


def test_non_admin_cannot_assign(client: TestClient, make_user, cliente_headers):
    target = make_user("target@example.com", UserRole.cliente)
    resp = client.post(_admin_assign_path(target.id), json=_plan_body(), headers=cliente_headers)
    assert resp.status_code == 403


def test_assign_to_unknown_client_404(client: TestClient, admin_headers):
    resp = client.post(_admin_assign_path(9999), json=_plan_body(), headers=admin_headers)
    assert resp.status_code == 404


def test_assign_to_admin_target_404(client: TestClient, make_user, admin_headers):
    other_admin = make_user("other-admin@studiomeridia.it", UserRole.admin)
    resp = client.post(_admin_assign_path(other_admin.id), json=_plan_body(), headers=admin_headers)
    # A plan is assigned to a cliente, not to studio staff.
    assert resp.status_code == 404


def test_assign_invalid_values_422(client: TestClient, make_user, admin_headers):
    cliente = make_user("x@example.com", UserRole.cliente)
    resp = client.post(
        _admin_assign_path(cliente.id),
        json={"name": "P", "daily_kcal": 0, "weeks": 0},
        headers=admin_headers,
    )
    assert resp.status_code == 422


def test_reassign_deactivates_previous(
    client: TestClient, session: Session, make_user, admin_headers
):
    cliente = make_user("gino@example.com", UserRole.cliente)
    first = client.post(
        _admin_assign_path(cliente.id),
        json=_plan_body("Piano A", 1400, 4),
        headers=admin_headers,
    ).json()
    second = client.post(
        _admin_assign_path(cliente.id),
        json=_plan_body("Piano B", 1600, 6),
        headers=admin_headers,
    ).json()
    assert first["id"] != second["id"]

    from app.repositories.nutrition_plan_repository import NutritionPlanRepository

    active = NutritionPlanRepository(session).get_active_by_client(cliente.id)
    assert active is not None
    assert active.name == "Piano B"
    # Exactly one active plan remains.
    all_plans = NutritionPlanRepository(session).list_by_client(cliente.id)
    assert sum(1 for p in all_plans if p.is_active) == 1


def _auth_header(user) -> dict:
    from app.core.security import create_access_token

    token = create_access_token(subject=str(user.id), role=user.role.value)
    return {"Authorization": f"Bearer {token}"}


def test_client_sees_own_active_plan(client: TestClient, make_user, admin_headers):
    cliente = make_user("owner@example.com", UserRole.cliente)
    client.post(_admin_assign_path(cliente.id), json=_plan_body(), headers=admin_headers)
    resp = client.get(ME_PLAN, headers=_auth_header(cliente))
    assert resp.status_code == 200
    assert resp.json()["name"] == "Dimagrimento 1400 kcal"


def test_client_without_plan_gets_404_locked(client: TestClient, cliente_headers):
    resp = client.get(ME_PLAN, headers=cliente_headers)
    assert resp.status_code == 404
    # Italian detail; the client renders a locked box, not an error.
    assert resp.json()["detail"]


def test_plan_isolation_between_clients(client: TestClient, make_user, admin_headers):
    a = make_user("a@example.com", UserRole.cliente)
    b = make_user("b@example.com", UserRole.cliente)
    client.post(_admin_assign_path(a.id), json=_plan_body("Piano di A"), headers=admin_headers)
    # b has no plan of their own → 404, and never sees a's plan.
    resp = client.get(ME_PLAN, headers=_auth_header(b))
    assert resp.status_code == 404
