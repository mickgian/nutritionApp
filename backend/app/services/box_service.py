"""Weekly meal box business logic: read (get-or-create) a client's box.

A box exists only once a plan is assigned (no plan → 404, the client renders a
locked box). For a valid week the box is get-or-created idempotently from the
shared meal catalog (seeded on first use, Rule 9): 14 cells = 7 weekdays x
lunch/dinner, rotated by week so each week of the plan differs. Ownership is
enforced by the caller passing ``client_id`` from the JWT.
"""

import structlog
from fastapi import HTTPException, status

from app.models.meal import Meal, MealSlot
from app.models.meal_box import BoxItem, MealBox
from app.repositories.box_repository import BoxRepository
from app.repositories.meal_repository import MealRepository
from app.repositories.nutrition_plan_repository import NutritionPlanRepository
from app.schemas.box import BoxItemRead, BoxRead, MealRead
from app.services.meal_catalog_seed import build_catalog

logger = structlog.get_logger(__name__)

_WEEKDAYS = 7


class BoxService:
    def __init__(
        self,
        plans: NutritionPlanRepository,
        meals: MealRepository,
        boxes: BoxRepository,
    ) -> None:
        self.plans = plans
        self.meals = meals
        self.boxes = boxes

    def get_box(self, client_id: int, week_index: int) -> BoxRead:
        plan = self.plans.get_active_by_client(client_id)
        if plan is None:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Nessun piano attivo")
        if week_index >= plan.weeks:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND, detail="Settimana non disponibile"
            )
        self._ensure_catalog()
        box = self.boxes.get_box(client_id, plan.id, week_index)
        if box is None:
            box = self._generate(client_id, plan.id, week_index)
        return self._assemble(box.week_index, self.boxes.list_items(box.id))

    def _ensure_catalog(self) -> None:
        if self.meals.count() == 0:
            self.meals.bulk_create(build_catalog())
            logger.info("meal_catalog_seeded", operation="seed_catalog")

    def _generate(self, client_id: int, plan_id: int, week_index: int) -> MealBox:
        lunch = self.meals.list_by_slot(MealSlot.lunch)
        dinner = self.meals.list_by_slot(MealSlot.dinner)
        items: list[BoxItem] = []
        for weekday in range(_WEEKDAYS):
            offset = week_index * _WEEKDAYS + weekday
            items.append(
                BoxItem(
                    meal_id=lunch[offset % len(lunch)].id,
                    weekday=weekday,
                    slot=MealSlot.lunch,
                )
            )
            items.append(
                BoxItem(
                    meal_id=dinner[offset % len(dinner)].id,
                    weekday=weekday,
                    slot=MealSlot.dinner,
                )
            )
        box = MealBox(client_id=client_id, plan_id=plan_id, week_index=week_index)
        created = self.boxes.create_box(box, items)
        logger.info(
            "meal_box_created",
            operation="generate_box",
            user_id=client_id,
            plan_id=plan_id,
            week_index=week_index,
            box_id=created.id,
        )
        return created

    def _assemble(self, week_index: int, items: list[BoxItem]) -> BoxRead:
        # Batch-load every referenced meal in one query (was one get_by_id per cell).
        by_id: dict[int, Meal] = {
            meal.id: meal for meal in self.meals.list_by_ids({item.meal_id for item in items})
        }
        read_items = [
            BoxItemRead(
                weekday=item.weekday,
                slot=item.slot,
                meal=MealRead.model_validate(by_id[item.meal_id]),
            )
            for item in items
            if item.meal_id in by_id
        ]
        return BoxRead(week_index=week_index, items=read_items)
