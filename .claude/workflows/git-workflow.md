# Git Workflow (Meridia)

The monorepo holds both stacks. A single feature branch and a single PR cover
backend (`backend/`) and KMP client changes together.

## Branch model

- **Default branch:** the repository default (feature PRs target it).
- **Feature branches:** `DEV-XXX-descriptive-name`, cut from the default branch.
- One branch per task; reserve it via `.claude/locks/branch-locks.template.json`
  (managed by @ottavio) to avoid two agents colliding on the same files.

## Standard sequence

```bash
git checkout <default-branch> && git pull
git checkout -b DEV-XXX-descriptive-name

# implement (TDD): tests first, then code
cd backend && uv run pytest            # backend
./gradlew :shared:allTests             # KMP
cd backend && uv run ruff format . && uv run ruff check --fix .

git add .
git status && git diff --staged
```

Then follow `human-in-the-loop-git.md` for who commits/pushes.

## Commit messages

- Imperative, scoped: `DEV-XXX: add appointment booking endpoint`.
- Reference the task id; keep the body to what changed and why.
- Do NOT include internal model identifiers or secrets.

## Before opening a PR (@silvano)

- All tests green on both stacks touched by the diff.
- `@collaudatore` returned `VERDICT: PASS` on the feature rubric.
- Migrations (if any) apply and reverse cleanly (`alembic upgrade head` /
  `downgrade -1`).
- PR description lists backend + frontend changes and any migration.
