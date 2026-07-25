---
description: Manually grade the current feature against a rubric via @collaudatore (terminal gate). Use for re-grading after a fix, grading a feature from a prior session, or grading on demand. The same grader fires automatically when you announce feature completion AND the diff has feature-shape — this command is the manual override.
allowed-tools: [Read, Grep, Glob, Bash, Task]
---

Invoke the `@collaudatore` subagent to grade the current branch's changes.

## What to do

1. Determine the rubric to use:
   - If `$ARGUMENTS` is a rubric id (e.g. `feature-implementation`, `bug-fix`), use `.claude/rubrics/<id>.yaml`.
   - If empty, default to `.claude/rubrics/feature-implementation.yaml`.
   - If the value doesn't match a file in `.claude/rubrics/`, list the available rubrics and stop.

2. Compute the changed files:
   ```bash
   git diff --name-status HEAD
   git diff --name-status $(git merge-base HEAD origin/HEAD 2>/dev/null || git merge-base HEAD master)...HEAD
   ```

3. Invoke the `@collaudatore` subagent with this prompt:
   > ITERATION 1 of 4
   > Rubric: `<resolved-rubric-path>`
   > Changed files: `<paste git diff output>`
   > Grade per the rubric. Run every check yourself with Bash. Output must end with `VERDICT: PASS | FAIL | ESCALATE`.

4. **If VERDICT is FAIL:** route the Italian remediation list to the right worker:
   - Backend file remediation → `@ezio`
   - KMP / Compose remediation → `@livia`
   - Test gaps → `@clelia`
   - UX gaps → `@gioia`
   - Migration / schema issues → `@primo`
   - Security / isolation issues → `@severino`

   After the worker(s) apply fixes, re-invoke `@collaudatore` with `ITERATION 2 of 4`. Repeat up to 4 iterations.

5. **If VERDICT is PASS:** report success and ask the user if they want `@silvano` to open a PR (after the human commits & pushes).

6. **If VERDICT is ESCALATE (iteration 4 + FAIL):** invoke `@silvano` to open a GitHub issue titled `Rubric escalation: <feature> — 4 iterations exhausted` with the grading report attached. Halt feature work and ask the user to review the rubric.

## Notes

- The grader runs all checks itself with Bash — it does NOT trust that `@clelia`, `@severino`, etc. did the work.
- You (the orchestrator) own the iteration counter. Subagents are stateless across calls.
- For calibration against a past merged PR, check out the merge commit first, then run this command (see `.claude/rubrics/README.md`).
