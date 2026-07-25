# CLAUDE.md — Meridia

Guidance for Claude Code (and every subagent) when working in this repository.

---

## 1. What Meridia is

Meridia is the app for **Studio Meridia**, a nutrition studio (Pachino, Sicily).
Clients book nutrition consultations, receive a personalized meal plan, and order a
weekly **meal box** (14 ready meals). The studio (admin / nutritionist) manages
availability, assigns plans, and sends behavior-profiled push notifications.

The interactive reference is the HTML demo at **`docs/demo/nutriodemo.html`**. The
plan to turn it into a real app lives in **`docs/tasks/DEMO_TO_KMP_TASKS.md`**.

**Language:** the product is Italian. All user-facing text and API error messages
are in **Italian**. Code, comments, logs, and tests are in English.

---

## 2. Monorepo layout

A single repo holds both stacks.

```
/                      Gradle Kotlin Multiplatform project (the client)
├── shared/            Compose Multiplatform UI + ViewModels + Ktor networking
│   └── src/commonMain/kotlin/com/meridia/shared/{screens,viewModels,models,network,auth,storage,utils}
├── androidApp/  iosApp/  webApp/  desktopApp/    KMP targets
├── backend/           Python 3.12 + FastAPI service
│   ├── app/{core,models,schemas,api,services,repositories}
│   ├── alembic/       migrations
│   ├── tests/         pytest (mirrors app/)
│   └── docker-compose.yml    local PostgreSQL
├── docs/tasks/        the demo → app implementation plan
└── .claude/           agents, hooks, rubrics, commands, workflows (this config)
```

---

## 3. Tech stack

**Backend** — Python 3.12 · FastAPI · **SQLModel** (never SQLAlchemy
`declarative_base`) · Pydantic v2 · PostgreSQL 16 · Alembic · JWT (HS256) + bcrypt ·
structlog · **uv** · **ruff** · **pytest**.

**Frontend** — Kotlin Multiplatform · Compose Multiplatform · **MVVM** (ViewModels
expose `StateFlow<UiState>`) · Ktor client · kotlinx.serialization · kotlinx.coroutines ·
targets Android / iOS / Web (Wasm/JS) / Desktop (JVM).

There is **no** LLM / RAG / vector DB / message broker. Don't add heavyweight
infrastructure without @egidio's approval.

---

## 4. The agent team

Named subagents live in `.claude/agents/` (roster in `.claude/subagent-names.json`).
Invoke by name with `@`.

| Agent | Role | Use for |
|-------|------|---------|
| **@egidio** | Architect — **VETO POWER** | Architecture, ADRs, stack/dependency approval, schema & contract review |
| **@ottavio** | Scrum Master | Sprint planning, task coordination, branch-lock reservations |
| **@mario** | Business Analyst | Requirements elicitation, impact analysis, task specs |
| **@primo** | Database Designer | PostgreSQL schema, SQLModel tables, Alembic migrations, indexes |
| **@ezio** | Backend Expert | FastAPI endpoints, services, repositories, auth |
| **@livia** | Frontend Expert | KMP + Compose screens, ViewModels, Ktor integration |
| **@clelia** | Test Engineer | pytest (backend) + kotlin.test (KMP), TDD, coverage |
| **@severino** | Security Auditor | GDPR (health data), JWT/auth, per-user isolation, payment-data, secrets |
| **@valerio** | Performance Engineer | API latency, query perf, KMP/Compose perf |
| **@gioia** | UX Reviewer | End-to-end UX audits of the Italian flows |
| **@silvano** | DevOps | GitHub PRs, GitHub Actions CI/CD, Docker, builds |
| **@tiziano** | Debugging Expert | Root-cause analysis across both stacks |
| **@collaudatore** | Feature Inspector — terminal gate | Grades finished features vs `.claude/rubrics/` |

### Typical workflow for a feature

```
@mario (requirements + impact)
   → @egidio (architecture review; may VETO)
      → @primo (schema/migration, if DB touched)
         → @ezio (backend, TDD)  +  @livia (KMP client)
            → @clelia (tests)  → @severino (security)  → @gioia (UX)
               → @collaudatore (grade vs rubric; loop ≤4)
                  → @silvano (PR, after the human commits/pushes)
```

Consult **@egidio before** any significant architectural change, new dependency, or
schema/contract change — he can veto autonomously and the veto stands until a human
overrides it.

---

## 5. Critical rules (non-negotiable)

1. **TDD.** Write the failing test first (RED → GREEN → REFACTOR) for backend
   service/API work. Target **≥80% coverage on new/changed code**.
2. **SQLModel only** — never SQLAlchemy `declarative_base`. Register every model in
   `backend/app/models/__init__.py` so Alembic autogenerate sees it.
3. **Italian** for every user-facing string and API error `detail`.
4. **Per-user data isolation.** A `cliente` reads/writes only their own rows
   (appointments, orders, plan, notifications, credits): every owned query filters
   by `client_id == current_user.id`. Studio endpoints require the `admin` role
   (`require_admin` / `AdminUser` in `backend/app/api/deps.py`).
5. **No swallowed errors.** Every caught exception is logged (structlog) with
   context (`user_id`, `operation`, `error_type`). No bare `except`, no `print`.
6. **No 500s on expected conditions.** Validate input; raise 4xx (400/401/403/404/
   409/422) with an Italian message instead.
7. **Money is integer cents.** Datetimes are timezone-aware UTC.
8. **MVVM.** No network calls or business logic inside a `@Composable`. Every
   data-loading screen renders loading / empty / error / content states.
9. **No manual ops on deployed environments.** Fixes ship as code (migrations,
   startup tasks); throwaway artifacts are logged in `.claude/one-time-fixes/`.
10. **Human-in-the-loop git** by default (agents stage; a human commits/pushes) —
    except in autonomous sessions where the human has explicitly delegated it.

### Size limits

| Backend | Max | Frontend | Max |
|---------|-----|----------|-----|
| Route handler | 30 lines | Compose screen file | 250 lines |
| Service class | 200 lines | Single `@Composable` | 120 lines |
| Python file | 400 lines | ViewModel | 200 lines |

---

## 6. Commands

**Backend** (run inside `backend/`):
```bash
docker compose up -d db                          # local PostgreSQL (port 5432)
uv venv && uv pip install -e ".[dev]"            # first-time setup
cp .env.example .env                             # set a strong SECRET_KEY
uv run uvicorn app.main:app --reload             # http://localhost:8000/docs
uv run pytest                                    # tests
uv run pytest --cov=app --cov-report=term-missing
uv run ruff format . && uv run ruff check --fix .
uv run alembic revision --autogenerate -m "..."  # then review the file
uv run alembic upgrade head                       # apply
uv run alembic downgrade -1                        # verify reversibility
```

**Frontend** (run at repo root):
```bash
./gradlew :shared:compileKotlinMetadata          # fast common-code compile check
./gradlew :shared:allTests                        # multiplatform unit tests
./gradlew :androidApp:assembleDebug               # Android build
```

---

## 7. Enforcement (hooks & rubrics)

`.claude/settings.json` wires hooks (see `.claude/workflows/permissions.md`):

- **SessionStart** injects project context.
- **PreToolUse (Edit/Write)** protects secrets/lockfiles/migrations and warns on
  missing tests.
- **PreToolUse (Bash)** requires an @egidio review marker before `git commit` and
  blocks destructive commands.
- **PostToolUse** auto-formats with ruff / ktlint.
- **Stop** blocks session end if a feature-shape diff + a completion claim never ran
  **@collaudatore**. Override a false positive with `/grade-skip <reason>`.

**@collaudatore** grades finished features against `.claude/rubrics/*.yaml` and
returns `VERDICT: PASS | FAIL | ESCALATE`. Run it manually with `/grade-feature`.

---

## 8. Git

- Branch from the default branch: `DEV-XXX-descriptive-name`.
- One feature branch / one PR covers backend + KMP changes together (monorepo).
- Commit messages: imperative, scoped (`DEV-XXX: add appointment booking`); no
  secrets, no internal model identifiers.
- See `.claude/workflows/git-workflow.md` and `human-in-the-loop-git.md`.
