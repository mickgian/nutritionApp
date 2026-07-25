# Human-in-the-Loop Git Workflow (Meridia)

**Authority:** Michele (the human) authorizes all commits and pushes.

---

## Git operations authority

**Agents CAN:**
- ✅ `git checkout -b DEV-XXX-name` — create feature branches
- ✅ `git checkout <default-branch>` / `git pull` — update from remote
- ✅ `git add .` / `git add <files>` — stage changes
- ✅ `git status`, `git diff` — inspect
- ✅ Read/Write/Edit files, run tests and linters

**Agents CANNOT (by default):**
- ❌ `git commit` — the human commits
- ❌ `git push` — the human pushes

> In an autonomous remote session where the human has explicitly delegated
> commit+push (as in this repo's task instructions), the orchestrator may commit
> and push to the designated branch. The default studio workflow below is the
> human-gated one used for day-to-day feature work.

**The human MUST (default workflow):**
- ✅ Review staged changes
- ✅ Execute `git commit` and `git push`
- ✅ Signal completion (e.g. "DEV-012-booking-endpoint pushed")

---

## Flow

### 1. Agent prepares changes
```bash
git checkout <default-branch> && git pull
git checkout -b DEV-XXX-descriptive-name
# ... Write/Edit files, run: cd backend && uv run pytest ; ./gradlew :shared:allTests
git add .
git status && git diff --staged
```

Agent signals:
```
Changes staged, ready for commit:

Task: DEV-XXX — <brief>
Branch: DEV-XXX-descriptive-name
Scope: backend | frontend | both

Staged files:
- backend/app/services/booking_service.py (new)
- backend/tests/services/test_booking_service.py (tests)
- shared/src/commonMain/kotlin/.../BookingViewModel.kt

Tests: ✅ all passing    Lint: ✅ ruff/ktlint    Coverage: ✅ >=80% new code

Waiting for the human to commit and push.
```

### 2. Human commits & pushes
```bash
git commit -m "DEV-XXX: <summary>"
git push -u origin DEV-XXX-descriptive-name
```

### 3. @silvano opens the PR (single PR covers backend + KMP in this monorepo)

---

## Branch naming

`DEV-XXX-descriptive-name` (e.g. `DEV-012-appointment-booking`,
`DEV-030-mealbox-checkout`). Missing the description or the ticket is rejected.

## PR target

PRs target the repository's **default branch**. @silvano creates the PR only
after the human has pushed.
