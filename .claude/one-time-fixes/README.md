# One-time fixes — code artifacts that exist to be removed

This directory tracks every **one-time / throwaway code artifact** the project
ships to fix a deployed-environment issue without manual ops. Each entry is the
audit trail for a single piece of scaffolding that must eventually be deleted.

Created per the CLAUDE.md rule "No manual ops on deployed environments".

## When to add an entry

Anytime a change introduces code whose purpose is to be deleted later:

- An Alembic **data migration** that backfills / cleans up historical rows (vs a
  schema migration, which is permanent)
- A `workflow_dispatch` GitHub Action used to run a one-off task
- A self-disabling startup task that exists only for first-deploy cleanup
- A short-lived feature flag that gates throwaway behavior
- Any seed script or compatibility shim with a known end-of-life

## File format

```
.claude/one-time-fixes/<YYYY-MM-DD>-<slug>.md

# <one-line title>

- **What it does:** ...
- **Why it's one-time:** ...
- **Implemented in:** <file paths> (and commit SHA once committed)
- **Trigger:** auto on next migrate / workflow_dispatch / startup hook
- **Remove when:** <concrete date OR observable condition>
- **Removed in:** <PR# / commit SHA> — filled when the cleanup actually ships
```

## Auditing open vs closed

```bash
grep -L "^- \*\*Removed in:\*\* " .claude/one-time-fixes/*.md | grep -v README
```

prints every entry that has NOT been closed yet. Add this to sprint planning /
release prep so entries do not pile up.

## Hard rules

- One file per fix (a change may introduce multiple).
- Never edit a closed entry to "reuse" it — create a new file with today's date.
- `Remove when:` must be concrete — a date or an observable condition.
- When the cleanup ships, fill in `Removed in:` and leave the rest as the record.
