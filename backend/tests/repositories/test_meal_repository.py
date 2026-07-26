"""Repository tests for the meal catalog access (DEV-094 perf pass).

``count`` must issue a COUNT (not load rows) and ``list_by_ids`` must batch-fetch
in one query rather than one-per-id, so the box read (GET /me/box) avoids an N+1.
"""

from sqlmodel import Session

from app.models.meal import Meal, MealSlot
from app.repositories.meal_repository import MealRepository


def _meal(title: str, slot: MealSlot = MealSlot.lunch) -> Meal:
    return Meal(title=title, slot=slot, kcal=400, protein_g=30, carb_g=20, fat_g=10)


def test_count_reports_number_of_rows(session: Session):
    repo = MealRepository(session)
    assert repo.count() == 0
    repo.bulk_create([_meal("Insalata"), _meal("Pollo", MealSlot.dinner)])
    assert repo.count() == 2


def test_list_by_ids_batch_fetches_only_requested(session: Session):
    repo = MealRepository(session)
    repo.bulk_create([_meal("A"), _meal("B"), _meal("C")])
    all_meals = repo.list_by_slot(MealSlot.lunch)
    wanted = {all_meals[0].id, all_meals[2].id}

    fetched = repo.list_by_ids(wanted)

    assert {m.id for m in fetched} == wanted


def test_list_by_ids_empty_returns_empty_without_querying(session: Session):
    repo = MealRepository(session)
    assert repo.list_by_ids([]) == []
