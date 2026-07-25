# Permissions & Guardrails (Meridia)

How the `.claude` guardrails constrain what agents may do. Source of truth:
`.claude/settings.json` (`permissions.allow` + `hooks`).

## Pre-allowed commands (no prompt)

Read-only and routine build/test commands are allowlisted in `settings.json`:
`uv run pytest`, `uv run ruff`, `uv run alembic`, `./gradlew …`, `git status/log/
diff/branch/add/checkout/fetch/pull`, and `docker compose …`. Anything else
prompts for approval.

## Enforcement hooks

| Hook | Event | Effect |
|------|-------|--------|
| `session-start.sh` | SessionStart | Injects project context + agent workflow |
| `github-issue-context.sh` | UserPromptSubmit | Injects GitHub issue context (web only) |
| `protect-files.sh` | PreToolUse Edit/Write | Blocks edits to secrets, lockfiles, generated migrations, signing keys |
| `tdd-check.sh` | PreToolUse Edit/Write | Warns when editing `backend/app/` code with no matching test |
| `architect-review.sh` | PreToolUse Bash | Blocks `git commit` until an @egidio review marker exists for the staged diff |
| `bash-validator.sh` | PreToolUse Bash | Blocks destructive commands (`rm -rf /`, `DROP DATABASE`, force-push to main, …) |
| `auto-format.sh` | PostToolUse Edit/Write | Runs ruff (Python) / ktlint (Kotlin) on the edited file |
| `check-collaudatore.sh` | Stop | Blocks session end if a feature-shape diff + completion claim never ran @collaudatore |

## Overriding a gate

- **Architect gate:** invoke @egidio to review `git diff --cached`, then
  `mkdir -p /tmp/claude && touch /tmp/claude/architect-review-<hash>` (the hook
  prints the exact path), and retry the commit.
- **Grader gate false positive:** run `/grade-skip <reason>` to opt out for today
  (refactor, docs, infra that merely looks like a feature).

## Protected files (never edited by agents)

`.env*`, `backend/alembic/versions/*` (generated), `local.properties`,
`secret.properties`, keystores/`*.jks`, `google-services.json`,
`GoogleService-Info.plist`, lockfiles. Edit these by hand.

## Roles inside the app (not to be confused with agent permissions)

The app itself has two user roles enforced in `backend/app/api/deps.py`:
`cliente` (owns only their own data) and `admin` (studio staff). Security review
of these boundaries is @severino's job.
