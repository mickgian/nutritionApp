#!/usr/bin/env bash
# Stop hook: blocks session-end if Claude announced feature completion but
# @collaudatore never ran to grade the work.
#
# Two conditions BOTH must be true to block:
#   A) Final assistant message contains a completion phrase (EN or IT)
#   B) Diff has feature-shape:
#        - new file under backend/app/api/ or a new KMP screen, OR
#        - cross-stack change touching BOTH backend/ AND a KMP module (shared/, androidApp/, ...)
#
# Bypass: /grade-skip <reason> writes .claude/locks/grade-skip-YYYYMMDD.flag
#
# Exit codes (Claude Code Stop hook contract):
#   0 — allow stop
#   2 — block; print JSON {decision,reason} to stdout

set -uo pipefail

if ! command -v jq >/dev/null 2>&1; then
    echo '{"decision":"allow","reason":"check-collaudatore.sh: jq not installed; skipping grader gate"}' >&2
    exit 0
fi
if ! command -v git >/dev/null 2>&1; then exit 0; fi

PROJECT_DIR="${CLAUDE_PROJECT_DIR:-$(pwd)}"
cd "$PROJECT_DIR" 2>/dev/null || exit 0

INPUT="$(cat)"
TRANSCRIPT="$(jq -r '.transcript_path // empty' <<<"$INPUT")"
[[ -z "$TRANSCRIPT" || ! -f "$TRANSCRIPT" ]] && exit 0

# ---------- Condition B: feature-shape changes ----------
feature_shape() {
    local mode="$1"  # "" for worktree vs HEAD, "--cached" for staged
    # A) new backend API file or new KMP screen
    git diff $mode --name-status HEAD 2>/dev/null | awk '
        $1=="A" && ($2 ~ /^backend\/app\/api\// || $2 ~ /screens\/.*\.kt$/) {print $2; found=1; exit}
        END{ if(!found) exit 1 }'
}

NEW_FEATURE_FILES="$(feature_shape "" || true)"

# B) cross-stack: changes in BOTH backend/ AND a KMP module
kmp_changed() {
    git diff "$@" --name-only HEAD 2>/dev/null | grep -E '^(shared|androidApp|iosApp|webApp|desktopApp)/' | head -1 || true
}
BACKEND_CHANGED="$(git diff --name-only HEAD 2>/dev/null | grep -E '^backend/' | head -1 || true)"
FRONTEND_CHANGED="$(kmp_changed)"
CROSS_STACK=""
[[ -n "$BACKEND_CHANGED" && -n "$FRONTEND_CHANGED" ]] && CROSS_STACK="yes"

# Also consider staged changes
if [[ -z "$NEW_FEATURE_FILES" && -z "$CROSS_STACK" ]]; then
    NEW_FEATURE_FILES="$(git diff --cached --name-status 2>/dev/null | awk '
        $1=="A" && ($2 ~ /^backend\/app\/api\// || $2 ~ /screens\/.*\.kt$/) {print $2; exit}')"
    BACKEND_STAGED="$(git diff --cached --name-only 2>/dev/null | grep -E '^backend/' | head -1 || true)"
    FRONTEND_STAGED="$(git diff --cached --name-only 2>/dev/null | grep -E '^(shared|androidApp|iosApp|webApp|desktopApp)/' | head -1 || true)"
    [[ -n "$BACKEND_STAGED" && -n "$FRONTEND_STAGED" ]] && CROSS_STACK="yes"
fi

if [[ -z "$NEW_FEATURE_FILES" && -z "$CROSS_STACK" ]]; then
    exit 0  # not a feature, no grading needed
fi

# ---------- Opt-out for today ----------
SKIP_FLAG="$PROJECT_DIR/.claude/locks/grade-skip-$(date +%Y%m%d).flag"
[[ -f "$SKIP_FLAG" ]] && exit 0

# ---------- Condition A: completion claim in final assistant message ----------
FINAL_MSG="$(tac "$TRANSCRIPT" 2>/dev/null | grep -m1 '"type":"assistant"' | jq -r '.message.content // .content // empty' 2>/dev/null || true)"
if [[ -z "$FINAL_MSG" ]]; then
    FINAL_MSG="$(tail -n 200 "$TRANSCRIPT" 2>/dev/null)"
fi

COMPLETION_REGEX='ready to (commit|push)|ready for pr|feature( is)? (implemented|complete|ready|done)|implementation( is)? complete|all done|pronto per (il commit|push|essere committat|essere pushat)|implementazione complet[ao]|funzionalità( è)? pront[ao]|pronto per il pr'

if ! echo "$FINAL_MSG" | grep -iqE "$COMPLETION_REGEX"; then
    exit 0
fi

# ---------- Did @collaudatore run this session? ----------
if grep -q '"subagent_type":\s*"collaudatore"' "$TRANSCRIPT" 2>/dev/null \
   || grep -q '"subagent_type":\s*"feature-inspector"' "$TRANSCRIPT" 2>/dev/null \
   || grep -q '@collaudatore' "$TRANSCRIPT" 2>/dev/null; then
    exit 0
fi

# ---------- Block ----------
TRIGGER="cross-stack changes"
[[ -n "$NEW_FEATURE_FILES" ]] && TRIGGER="new feature file: $NEW_FEATURE_FILES"

jq -n --arg trigger "$TRIGGER" '{
  decision: "block",
  reason: ("Feature-shape changes detected (" + $trigger + ") + completion claim in your final message, but @collaudatore never ran. Invoke the @collaudatore subagent now to grade against .claude/rubrics/feature-implementation.yaml. Loop worker->grader until VERDICT: PASS or 4 iterations. If this is a false positive (refactor, docs, infra), run /grade-skip <reason> to opt out for today.")
}'
exit 2
