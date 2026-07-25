# Rubrics — Feature Completion Gates (Meridia)

Rubrics are YAML files defining what "good" looks like for a recurring task type.
The `@collaudatore` subagent grades work against the relevant rubric before a
feature is declared complete.

This implements the **Outcomes pattern**: a separate grader, explicit criteria, an
iteration loop, and a hard cap. It attacks the specific failure mode of
"implementation looks done but ships with gaps" — 500s on the API, a backend
endpoint with no screen consuming it, missing loading/empty/error states, broken
per-user data isolation, or English text where Italian was required.

## How a rubric is used

1. **Trigger.** When Claude announces a feature is "ready to commit/push" AND the
   diff has feature-shape (a new file under `backend/app/api/`, a new KMP screen,
   or changes spanning both `backend/` and a KMP module), the `Stop` hook
   `.claude/hooks/check-collaudatore.sh` blocks the stop and instructs Claude to
   invoke `@collaudatore`.
2. **Grading.** `@collaudatore` loads the rubric, runs every `check` itself with
   Bash (it does NOT trust that upstream agents did the work), and emits a
   per-criterion PASS/FAIL/N-A table with `file:line` evidence plus a final
   `VERDICT: PASS | FAIL | ESCALATE` line.
3. **Iteration.** On FAIL, Claude routes the Italian remediation list back to the
   worker (`@ezio` for backend, `@livia` for KMP, `@clelia` for tests, `@primo`
   for migrations, `@gioia` for UX) and re-grades. Max 4 iterations.
4. **Escalation.** On iteration 4 + FAIL, the grader emits `VERDICT: ESCALATE`.
   Claude invokes `@silvano` to open a GitHub issue and halts feature work — at
   that point the rubric itself is likely the bug and a human must look.

## Schema

```yaml
---
id: <unique-id>                  # lowercase-kebab, used in /grade-feature <id>
name: <Human Readable Name>
applies_to: [<scenario list>]    # informational
max_iterations: 4                # hard cap; do not raise above 4
pass_threshold: "..."            # human-readable rule used by the grader
criticality_weights: { critical: 3, high: 2, medium: 1 }

criteria:
  - id: UNIQUE_ID                # UPPER_SNAKE, referenced in evidence
    criticality: critical|high|medium
    msg_it: "..."                # Italian remediation message the user sees
    check: |                     # runnable instructions for the grader
      ...
    evidence_required: [...]     # optional: what file:line refs to expect
    applies_when: "..."          # optional: skip unless condition met
```

## Rules for writing rubrics

1. Every `check` must be runnable with the grader's read-only tools
   (`Read, Grep, Glob, Bash`).
2. `critical` criteria are blocking — any FAIL prevents PASS regardless of score.
3. Use `applies_when` for conditional criteria (e.g. `MIGRATION_SAFE` only applies
   if `backend/alembic/versions/` changed).
4. Italian for `msg_it` (it ends up in the chat the user reads); English for
   `id`, `check`, `applies_when`.
5. Cap iterations at 4. If more than 4 cycles are needed, fix the rubric, not the
   code.

## Files

- `feature-implementation.yaml` — new endpoint + new screen + cross-stack. Primary.
- `bug-fix.yaml` — regression test required, no hardcoded values, root-cause check.

To add a rubric, copy one, change `id`, define criteria, and register the slug in
`.claude/commands/grade-feature.md` if you want it selectable.
