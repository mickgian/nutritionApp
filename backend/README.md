# Meridia Backend

FastAPI + SQLModel + PostgreSQL backend for the Meridia nutrition studio app.
It serves the Kotlin Multiplatform clients (Android / iOS / Web / Desktop) that
live in the rest of this monorepo.

## Stack

| Concern | Choice |
|---------|--------|
| Language | Python 3.12 |
| Web framework | FastAPI |
| ORM / models | SQLModel (never SQLAlchemy `declarative_base`) |
| Migrations | Alembic |
| Database | PostgreSQL 16 (Docker for local dev) |
| Validation | Pydantic v2 |
| Auth | JWT (HS256) + bcrypt password hashing |
| Logging | structlog (structured JSON in prod) |
| Deps / venv | uv |
| Lint / format | ruff |
| Tests | pytest (+ pytest-asyncio, httpx) |

## Layout

```
backend/
├── app/
│   ├── main.py              # FastAPI app + middleware + router wiring
│   ├── core/                # config, database, security, logging
│   ├── models/              # SQLModel tables (one concern per file)
│   ├── schemas/             # Pydantic request/response DTOs
│   ├── api/
│   │   ├── deps.py          # current-user / role dependencies
│   │   └── v1/              # versioned routers (thin handlers)
│   ├── services/            # business logic
│   └── repositories/        # DB access, per-user ownership filters
├── alembic/                 # migrations
├── tests/                   # mirrors app/ layout
├── docker-compose.yml       # local PostgreSQL
└── pyproject.toml
```

## Getting started

```bash
cd backend

# 1. Start the database
docker compose up -d db

# 2. Create a virtualenv and install deps (uv)
uv venv
uv pip install -e ".[dev]"

# 3. Configure environment
cp .env.example .env
# edit .env — set a strong SECRET_KEY

# 4. Run migrations (once you have created models)
uv run alembic upgrade head

# 5. Run the API (http://localhost:8000/docs)
uv run uvicorn app.main:app --reload
```

## Everyday commands

```bash
uv run pytest                                   # tests
uv run pytest --cov=app --cov-report=term-missing
uv run ruff format . && uv run ruff check --fix .
uv run alembic revision --autogenerate -m "add appointments"
uv run alembic upgrade head
uv run alembic downgrade -1                      # verify rollback
```

## Conventions

- **TDD**: write the failing test first (RED → GREEN → REFACTOR).
- **Coverage**: ≥80% on new/changed code.
- **Italian** for every user-facing string and API error `detail`.
- **User-data isolation**: a `cliente` may only access their own rows; studio
  endpoints require the `admin` role (see `app/api/deps.py`).
- **Structured logging**: every caught exception is logged with context; never
  swallow errors silently.
- **Money**: store as integer minor units (never floats) in the domain schema.
- Size limits: route handler < 30 lines, service class < 200, file < 400.

The scaffold ships a `User` and a starter `Appointment` model plus health
endpoints so the app boots and tests pass. The full domain schema (appointments,
availability, plans, meals, boxes, orders, payments, notifications) is built
task-by-task per `docs/tasks/DEMO_TO_KMP_TASKS.md`, designed by **@primo** and
implemented by **@ezio** under **@egidio**'s architectural review.
