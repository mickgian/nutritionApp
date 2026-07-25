#!/bin/bash
# Auto Format Hook - Formats code after edits
# Runs on: PostToolUse (Edit|Write)

INPUT=$(cat)
FILE_PATH=$(echo "$INPUT" | jq -r '.tool_input.file_path // empty')

if [[ -z "$FILE_PATH" ]]; then
    exit 0
fi
if [[ ! -f "$FILE_PATH" ]]; then
    exit 0
fi

# Python files - use ruff (run from the backend dir so config is picked up)
if [[ "$FILE_PATH" == *.py ]]; then
    if command -v uv &> /dev/null && [[ "$FILE_PATH" == *"backend/"* ]]; then
        (cd "$(dirname "$FILE_PATH")" && uv run ruff format "$FILE_PATH" 2>/dev/null; \
         uv run ruff check --fix "$FILE_PATH" 2>/dev/null)
        echo "Formatted: $FILE_PATH (ruff)" >&2
    elif command -v ruff &> /dev/null; then
        ruff format "$FILE_PATH" 2>/dev/null
        ruff check --fix "$FILE_PATH" 2>/dev/null
        echo "Formatted: $FILE_PATH (ruff)" >&2
    fi
fi

# Kotlin files - use ktlint if available (optional; no-op if not installed)
if [[ "$FILE_PATH" == *.kt || "$FILE_PATH" == *.kts ]]; then
    if command -v ktlint &> /dev/null; then
        ktlint --format "$FILE_PATH" 2>/dev/null
        echo "Formatted: $FILE_PATH (ktlint)" >&2
    fi
fi

exit 0
