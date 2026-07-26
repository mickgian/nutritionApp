"""Seed data for the shared meal catalog (DEV-031).

A pool of Italian ready-meals per slot; the box generator rotates through them so
each week of a plan gets a distinct 14-meal menu. Macros are integers (grams);
kcal is integer. Storage/reheat notes are shared, realistic Italian copy.
"""

from app.models.meal import Meal, MealSlot

_STORAGE = "Conservare in frigorifero (max 4 gradi) e consumare entro 3 giorni dal ritiro."
_REHEAT = "Riscaldare in microonde per 2-3 minuti o in padella a fuoco medio fino a temperatura."

# (title, emoji, kcal, protein_g, carb_g, fat_g)
_LUNCH: list[tuple[str, str, int, int, int, int]] = [
    ("Pollo alle erbe con quinoa", "🍗", 520, 42, 48, 16),
    ("Salmone al forno con patate", "🐟", 610, 38, 40, 30),
    ("Farro con verdure grigliate", "🥗", 450, 16, 70, 12),
    ("Tacchino e riso basmati", "🍚", 540, 44, 55, 14),
    ("Frittata di zucchine con insalata", "🍳", 480, 28, 22, 30),
    ("Orata con couscous alle verdure", "🐟", 560, 36, 50, 22),
    ("Ceci speziati e riso integrale", "🌱", 500, 20, 78, 12),
    ("Manzo magro con pure di patate", "🥩", 620, 46, 42, 28),
]

_DINNER: list[tuple[str, str, int, int, int, int]] = [
    ("Merluzzo al vapore con verdure", "🐟", 420, 40, 24, 16),
    ("Petto di pollo e broccoli", "🍗", 460, 48, 20, 18),
    ("Vellutata di legumi con crostini", "🥣", 400, 18, 58, 10),
    ("Tofu saltato con verdure", "🌱", 430, 26, 34, 20),
    ("Uova e spinaci con pane integrale", "🍳", 410, 26, 30, 20),
    ("Branzino con zucchine", "🐟", 440, 38, 18, 24),
    ("Insalata di lenticchie e feta", "🥗", 470, 24, 46, 22),
    ("Tacchino e verdure al forno", "🍗", 450, 44, 28, 16),
]


def _build(specs: list[tuple[str, str, int, int, int, int]], slot: MealSlot) -> list[Meal]:
    return [
        Meal(
            title=title,
            slot=slot,
            kcal=kcal,
            protein_g=protein,
            carb_g=carb,
            fat_g=fat,
            asset_ref=emoji,
            storage_note=_STORAGE,
            reheat_note=_REHEAT,
        )
        for (title, emoji, kcal, protein, carb, fat) in specs
    ]


def build_catalog() -> list[Meal]:
    """The full seed catalog (lunch + dinner), unsaved."""
    return _build(_LUNCH, MealSlot.lunch) + _build(_DINNER, MealSlot.dinner)
