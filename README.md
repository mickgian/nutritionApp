# Meridia

The app for **Studio Meridia**, a nutrition studio: clients book consultations,
receive a personalized meal plan, and order a weekly meal box; the studio manages
availability, plans, orders, and notifications.

This is a **polyglot monorepo**:

- **Frontend** — Kotlin Multiplatform + Compose Multiplatform, targeting
  **Android, iOS, Web (Wasm/JS), and Desktop (JVM)**. Lives at the repo root
  (`shared/`, `androidApp/`, `iosApp/`, `webApp/`, `desktopApp/`).
- **Backend** — Python 3.12 + **FastAPI** + **SQLModel** + **PostgreSQL**, in
  [`backend/`](backend/README.md).

The product language is **Italian**; code, comments, and tests are English.

---

## Repository layout

```
/
├── shared/            KMP shared module — Compose UI, ViewModels, Ktor networking
├── androidApp/  iosApp/  webApp/  desktopApp/    KMP target apps
├── backend/           FastAPI service (see backend/README.md)
├── docs/
│   ├── demo/nutriodemo.html          the interactive HTML design reference
│   └── tasks/DEMO_TO_KMP_TASKS.md    the demo → app implementation plan
├── CLAUDE.md          guidance for Claude Code + the agent team
└── .claude/           agents, hooks, rubrics, commands, workflows
```

## Tech stack

| Layer | Choice |
|-------|--------|
| Client | Kotlin Multiplatform, Compose Multiplatform, MVVM (StateFlow) |
| Networking | Ktor client + kotlinx.serialization |
| Backend | FastAPI, SQLModel, Pydantic v2 |
| Database | PostgreSQL 16 (Docker for local dev) |
| Migrations | Alembic |
| Auth | JWT (HS256) + bcrypt; roles `cliente` / `admin` |
| Backend tooling | uv, ruff, pytest, structlog |

## Getting started

**Backend** (`backend/`):
```bash
cd backend
docker compose up -d db
uv venv && uv pip install -e ".[dev]"
cp .env.example .env          # set a strong SECRET_KEY
uv run alembic upgrade head
uv run uvicorn app.main:app --reload   # http://localhost:8000/docs
uv run pytest
```

**Frontend** (repo root):
```bash
./gradlew :shared:allTests
./gradlew :androidApp:assembleDebug
```

## How this repo is built

Work is driven by the plan in
[`docs/tasks/DEMO_TO_KMP_TASKS.md`](docs/tasks/DEMO_TO_KMP_TASKS.md), turning each
flow of the [HTML demo](docs/demo/nutriodemo.html) into a tested, end-to-end feature.

A team of named Claude Code subagents (see [`CLAUDE.md`](CLAUDE.md) and
`.claude/subagent-names.json`) collaborates under an **architect with veto power**
(@egidio) and a **terminal quality gate** (@collaudatore) that grades every finished
feature against `.claude/rubrics/`. Conventions: TDD, ≥80% coverage on new backend
code, Italian UI, per-user data isolation, MVVM with full loading/empty/error states.

## Base URL & auth (client → backend)

- Base URL: `http://localhost:8000/api/v1`
- Auth endpoints: `POST /auth/register`, `POST /auth/login` (JWT), `GET /auth/me`
- Errors: `422` validation, `4xx` business logic, Italian `detail` messages
