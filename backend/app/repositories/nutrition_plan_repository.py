"""Data access for :class:`NutritionPlan`. Every query filters by ``client_id``."""

from sqlmodel import Session, select

from app.models.nutrition_plan import NutritionPlan


class NutritionPlanRepository:
    def __init__(self, session: Session) -> None:
        self.session = session

    def get_active_by_client(self, client_id: int) -> NutritionPlan | None:
        stmt = select(NutritionPlan).where(
            NutritionPlan.client_id == client_id,
            NutritionPlan.is_active == True,  # noqa: E712 (SQL boolean comparison)
        )
        return self.session.exec(stmt).first()

    def list_by_client(self, client_id: int) -> list[NutritionPlan]:
        stmt = (
            select(NutritionPlan)
            .where(NutritionPlan.client_id == client_id)
            .order_by(NutritionPlan.created_at.desc())
        )
        return list(self.session.exec(stmt).all())

    def deactivate_active(self, client_id: int) -> None:
        """Mark any currently-active plan for this client inactive (one active max)."""
        stmt = select(NutritionPlan).where(
            NutritionPlan.client_id == client_id,
            NutritionPlan.is_active == True,  # noqa: E712
        )
        for plan in self.session.exec(stmt).all():
            plan.is_active = False
            self.session.add(plan)

    def create(self, plan: NutritionPlan) -> NutritionPlan:
        self.session.add(plan)
        self.session.commit()
        self.session.refresh(plan)
        return plan
