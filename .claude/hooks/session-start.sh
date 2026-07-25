#!/bin/bash
# Session Start Hook - Injects project context at session start
# Runs on: startup, resume, compact

cat << 'EOF'
## Meridia Session Context

Monorepo: KMP + Compose Multiplatform frontend (Android/iOS/Web/Desktop) at repo
root; Python/FastAPI backend under `backend/`. Single studio nutrition app,
Italian UI. Reference: the interactive HTML demo + docs/tasks/DEMO_TO_KMP_TASKS.md.

### Agent Workflow (see CLAUDE.md + .claude/subagent-names.json)
- @mario   -> requirements & impact analysis (3+ files, DB/API, auth/payments)
- @egidio  -> architecture review — HAS VETO POWER (consult before big changes)
- @primo   -> database design (SQLModel + Alembic)
- @ezio    -> backend implementation (FastAPI, TDD)
- @livia   -> frontend implementation (KMP + Compose + Ktor)
- @clelia  -> tests (pytest backend / kotlin.test KMP, target >=80% new code)
- @severino-> security & GDPR (health data, JWT, per-user isolation)
- @gioia   -> UX flow audit (Italian client/admin flows)
- @collaudatore -> terminal quality gate (grades vs .claude/rubrics/)
- @silvano -> PR creation & CI/CD (after human commits/pushes)

### Critical Rules
1. TDD: write tests FIRST (RED-GREEN-REFACTOR) for backend service/API work.
2. SQLModel ONLY — never SQLAlchemy declarative_base.
3. Italian for all user-facing text and API error `detail`.
4. Per-user data isolation: a cliente only touches their own rows; admin-only
   endpoints require the `admin` role.
5. Structured logging on every caught exception — never swallow errors.
6. Human-in-the-loop git: agents stage; a human commits & pushes.

### Quick Commands
- Backend tests:   cd backend && uv run pytest
- Backend format:  cd backend && uv run ruff format . && uv run ruff check --fix .
- Migration:       cd backend && uv run alembic revision --autogenerate -m "..."
- Frontend build:  ./gradlew :shared:compileKotlinMetadata
- Frontend tests:  ./gradlew :shared:allTests
EOF

echo ""
echo "### Recent Commits"
git log --oneline -5 2>/dev/null || echo "(not a git repo)"

echo ""
echo "### Current Branch"
git branch --show-current 2>/dev/null || echo "(not a git repo)"
