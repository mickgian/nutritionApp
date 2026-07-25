---
description: Opt out of @collaudatore grading for today. Use when the Stop hook flags a false positive (refactor, docs, infra change that happens to look like a feature). Creates a daily flag file the hook respects until midnight.
allowed-tools: [Bash]
---

Create a daily skip flag for the `@collaudatore` Stop hook.

## What to do

1. Treat `$ARGUMENTS` as the reason. If empty, ask the user for a one-line reason and stop.

2. Run:
   ```bash
   mkdir -p .claude/locks
   FLAG=".claude/locks/grade-skip-$(date +%Y%m%d).flag"
   REASON="$ARGUMENTS"
   echo "$(date -Iseconds) | $REASON" > "$FLAG"
   echo "$(date -Iseconds) | created: $FLAG | reason: $REASON" >> .claude/locks/grade-skip.log
   echo "Skipped grading until midnight. Flag: $FLAG"
   ```

3. Report the flag path to the user, plus a reminder that it auto-expires at midnight (the hook looks for today's date suffix).

## When to use

Legitimate uses:
- Refactor that happens to touch both `backend/` and a KMP module but adds no feature
- Docs-only commit that nonetheless modified a file under `backend/app/api/`
- Infrastructure/config change (Docker, GitHub Actions, Gradle) that landed in a feature directory

When NOT to use:
- You're tired of grading. (The cap is 4 iterations — if it fails more, the rubric is the bug; fix the rubric or escalate.)
- You think the rubric is wrong for this feature. (Fix the rubric for everyone, don't skip.)
