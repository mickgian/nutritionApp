#!/bin/bash
# TDD Check Hook - Warns when editing backend app code without a matching test
# Runs on: PreToolUse (Edit|Write)

INPUT=$(cat)
FILE_PATH=$(echo "$INPUT" | jq -r '.tool_input.file_path // empty')

if [[ -z "$FILE_PATH" ]]; then
    exit 0
fi

# Only check backend Python files under backend/app/ (not tests, not __init__.py)
if [[ "$FILE_PATH" != *"backend/app/"* ]]; then
    exit 0
fi
if [[ "$FILE_PATH" == *"__init__.py" ]]; then
    exit 0
fi
if [[ "$FILE_PATH" != *.py ]]; then
    exit 0
fi

# Map backend/app/services/foo.py -> backend/tests/services/test_foo.py
TEST_PATH=$(echo "$FILE_PATH" | sed 's|/app/|/tests/|' | sed 's|/\([^/]*\)\.py$|/test_\1.py|')

if [[ ! -f "$TEST_PATH" ]]; then
    cat << EOF
{
    "hookSpecificOutput": {
        "hookEventName": "PreToolUse",
        "additionalContext": "TDD REMINDER: No test file at $TEST_PATH\\n\\nBest practice: write the failing test FIRST, then implement. Consider creating: $TEST_PATH"
    }
}
EOF
fi

exit 0
