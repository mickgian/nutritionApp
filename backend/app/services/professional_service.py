"""Professional profile business logic: read the studio nutritionist + reviews.

The studio has exactly one professional (ADR-001). Reference data is seeded
idempotently on first read (``get_profile``) so the card always renders — no
manual ops on deployed environments (Rule 9). ``rating_avg`` is recomputed from
the reviews whenever they are seeded, kept as an int (stars x100) to stay
float-free (Rule 7).
"""

import structlog

from app.models.professional import Professional
from app.models.review import Review
from app.repositories.professional_repository import ProfessionalRepository
from app.schemas.professional import ProfessionalRead

logger = structlog.get_logger(__name__)

_SEED_BIO = (
    "Biologa nutrizionista a Pachino, aiuto le persone a raggiungere i propri "
    "obiettivi con percorsi sostenibili e personalizzati. Prima visita e visite "
    "di controllo in studio, con piani alimentari e box settimanali di pasti pronti."
)

# Seeded demo reviews (author_name captured at write time; author_id null).
_SEED_REVIEWS: list[tuple[str, int, str]] = [
    ("Giulia M.", 5, "Percorso chiaro e sostenibile: ho raggiunto i miei obiettivi senza stress."),
    (
        "Marco T.",
        5,
        "Il box settimanale mi ha semplificato la vita: mangio bene anche con poco tempo.",
    ),
    ("Federica R.", 4, "Grande professionalità e disponibilità. La consiglio davvero."),
    ("Antonio L.", 5, "Approccio scientifico e umano allo stesso tempo. Molto soddisfatto."),
]


class ProfessionalService:
    def __init__(self, repo: ProfessionalRepository) -> None:
        self.repo = repo

    def get_profile(self) -> ProfessionalRead:
        professional = self.repo.get_singleton()
        if professional is None:
            professional = self._seed()
        reviews = self.repo.list_reviews(professional.id)
        return ProfessionalRead.build(professional, reviews)

    def _seed(self) -> Professional:
        ratings = [rating for _, rating, _ in _SEED_REVIEWS]
        professional = Professional(
            full_name="Dott.ssa Anna Serra",
            title="Biologa Nutrizionista",
            bio=_SEED_BIO,
            rating_avg=round(sum(ratings) / len(ratings) * 100),
            reviews_count=len(ratings),
        )
        professional = self.repo.save(professional)
        self.repo.add_reviews(
            [
                Review(
                    professional_id=professional.id,
                    author_name=name,
                    rating=rating,
                    body=body,
                )
                for name, rating, body in _SEED_REVIEWS
            ]
        )
        logger.info(
            "professional_seeded",
            operation="professional_seed",
            professional_id=professional.id,
            reviews_count=len(ratings),
        )
        return professional
