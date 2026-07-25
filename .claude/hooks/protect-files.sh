#!/bin/bash
# Protect Files Hook - Blocks edits to sensitive / generated files
# Runs on: PreToolUse (Edit|Write)

INPUT=$(cat)
FILE_PATH=$(echo "$INPUT" | jq -r '.tool_input.file_path // empty')

if [[ -z "$FILE_PATH" ]]; then
    exit 0
fi

# Protected file patterns (secrets, lockfiles, generated migrations, keystores)
PROTECTED_PATTERNS=(
    ".env"
    ".env.local"
    ".env.production"
    "backend/alembic/versions/"
    ".git/"
    "local.properties"
    "secret.properties"
    ".keystore"
    ".jks"
    "GoogleService-Info.plist"
    "google-services.json"
    "uv.lock"
    "yarn.lock"
    "gradle/wrapper/gradle-wrapper.jar"
)

for pattern in "${PROTECTED_PATTERNS[@]}"; do
    if [[ "$FILE_PATH" == *"$pattern"* ]]; then
        echo "BLOCKED: Cannot edit protected file: $FILE_PATH" >&2
        echo "Pattern matched: '$pattern'" >&2
        echo "" >&2
        echo "Protected files (secrets, lockfiles, generated migrations, signing keys)" >&2
        echo "should be edited manually for safety." >&2
        exit 2
    fi
done

exit 0
