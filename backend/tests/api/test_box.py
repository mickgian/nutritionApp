"""Tests for the weekly meal box (DEV-031).

A client with an active plan reads their weekly box (``GET /api/v1/me/box?week=``):
14 meals (7 weekdays x lunch/dinner) with kcal + macros, seeded from the shared
meal catalog. No plan → 404 (locked box). The box is get-or-created idempotently;
weeks differ across the plan's cycle; out-of-range weeks 404; ownership isolated.
"""

from fastapi.testclient import TestClient
from sqlmodel import Session

from app.core.security import create_access_token
from app.models.user import UserRole

BOX = "/api/v1/me/box"


def _auth_header(user) -> dict:
    token = create_access_token(subject=str(user.id), role=user.role.value)
    return {"Authorization": f"Bearer {token}"}


def _menu_key(body: dict) -> list:
    return sorted((i["weekday"], i["slot"], i["meal"]["id"]) for i in body["items"])


def _assign_plan(client: TestClient, admin_headers, client_id: int, weeks: int = 4) -> None:
    resp = client.post(
        f"/api/v1/admin/clients/{client_id}/plan",
        json={"name": "Dimagrimento 1400 kcal", "daily_kcal": 1400, "weeks": weeks},
        headers=admin_headers,
    )
    assert resp.status_code == 201


def test_no_plan_returns_404_locked(client: TestClient, cliente_headers):
    resp = client.get(f"{BOX}?week=0", headers=cliente_headers)
    assert resp.status_code == 404
    assert resp.json()["detail"]


def test_box_has_14_meals_with_macros(client: TestClient, make_user, admin_headers):
    cliente = make_user("box1@example.com", UserRole.cliente)
    _assign_plan(client, admin_headers, cliente.id)
    resp = client.get(f"{BOX}?week=0", headers=_auth_header(cliente))
    assert resp.status_code == 200
    body = resp.json()
    assert body["week_index"] == 0
    items = body["items"]
    assert len(items) == 14
    weekdays = {i["weekday"] for i in items}
    slots = {i["slot"] for i in items}
    assert weekdays == set(range(7))
    assert slots == {"lunch", "dinner"}
    meal = items[0]["meal"]
    for field in ("title", "kcal", "protein_g", "carb_g", "fat_g"):
        assert field in meal
    assert meal["kcal"] > 0
    # A lunch cell carries a lunch meal, a dinner cell a dinner meal.
    for item in items:
        assert item["meal"]["slot"] == item["slot"]


def test_box_is_idempotent(client: TestClient, session: Session, make_user, admin_headers):
    cliente = make_user("box2@example.com", UserRole.cliente)
    _assign_plan(client, admin_headers, cliente.id)
    first = client.get(f"{BOX}?week=1", headers=_auth_header(cliente)).json()
    second = client.get(f"{BOX}?week=1", headers=_auth_header(cliente)).json()
    assert _menu_key(first) == _menu_key(second)

    from sqlmodel import select

    from app.models.meal_box import BoxItem, MealBox

    boxes = session.exec(select(MealBox).where(MealBox.client_id == cliente.id)).all()
    assert len(boxes) == 1
    items = session.exec(select(BoxItem).where(BoxItem.box_id == boxes[0].id)).all()
    assert len(items) == 14


def test_weeks_differ_across_cycle(client: TestClient, make_user, admin_headers):
    cliente = make_user("box3@example.com", UserRole.cliente)
    _assign_plan(client, admin_headers, cliente.id)
    w0 = client.get(f"{BOX}?week=0", headers=_auth_header(cliente)).json()
    w1 = client.get(f"{BOX}?week=1", headers=_auth_header(cliente)).json()
    menu0 = [(i["weekday"], i["slot"], i["meal"]["id"]) for i in w0["items"]]
    menu1 = [(i["weekday"], i["slot"], i["meal"]["id"]) for i in w1["items"]]
    assert menu0 != menu1


def test_week_out_of_range_404(client: TestClient, make_user, admin_headers):
    cliente = make_user("box4@example.com", UserRole.cliente)
    _assign_plan(client, admin_headers, cliente.id, weeks=4)
    resp = client.get(f"{BOX}?week=4", headers=_auth_header(cliente))
    assert resp.status_code == 404


def test_negative_week_422(client: TestClient, make_user, admin_headers):
    cliente = make_user("box5@example.com", UserRole.cliente)
    _assign_plan(client, admin_headers, cliente.id)
    resp = client.get(f"{BOX}?week=-1", headers=_auth_header(cliente))
    assert resp.status_code == 422


def test_box_isolation_between_clients(
    client: TestClient, session: Session, make_user, admin_headers
):
    a = make_user("boxa@example.com", UserRole.cliente)
    b = make_user("boxb@example.com", UserRole.cliente)
    _assign_plan(client, admin_headers, a.id)
    _assign_plan(client, admin_headers, b.id)
    client.get(f"{BOX}?week=0", headers=_auth_header(a))
    client.get(f"{BOX}?week=0", headers=_auth_header(b))

    from sqlmodel import select

    from app.models.meal_box import MealBox

    a_boxes = session.exec(select(MealBox).where(MealBox.client_id == a.id)).all()
    b_boxes = session.exec(select(MealBox).where(MealBox.client_id == b.id)).all()
    assert len(a_boxes) == 1 and len(b_boxes) == 1
    assert a_boxes[0].id != b_boxes[0].id
