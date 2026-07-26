"""Data access for :class:`MealBox` and :class:`BoxItem`. Filters by ``client_id``."""

from sqlmodel import Session, select

from app.models.meal_box import BoxItem, MealBox


class BoxRepository:
    def __init__(self, session: Session) -> None:
        self.session = session

    def get_box(self, client_id: int, plan_id: int, week_index: int) -> MealBox | None:
        stmt = select(MealBox).where(
            MealBox.client_id == client_id,
            MealBox.plan_id == plan_id,
            MealBox.week_index == week_index,
        )
        return self.session.exec(stmt).first()

    def list_items(self, box_id: int) -> list[BoxItem]:
        stmt = (
            select(BoxItem).where(BoxItem.box_id == box_id).order_by(BoxItem.weekday, BoxItem.slot)
        )
        return list(self.session.exec(stmt).all())

    def create_box(self, box: MealBox, items: list[BoxItem]) -> MealBox:
        self.session.add(box)
        self.session.commit()
        self.session.refresh(box)
        for item in items:
            item.box_id = box.id
            self.session.add(item)
        self.session.commit()
        return box
