#!/bin/bash
# GitHub Issue Context Hook
# Runs on: UserPromptSubmit (web only)
# Purpose: Detect GitHub issue references and inject issue context + branch tips.

# ----- WEB-ONLY CHECK -----
if [ "$CLAUDE_CODE_REMOTE" != "true" ]; then
    exit 0
fi

# ----- CONFIGURATION -----
DEFAULT_REPO="mickgian/nutritionapp"
REPO_NAME="nutritionapp"

# ----- READ USER PROMPT -----
if [ -t 0 ]; then
    exit 0
fi

INPUT=$(cat)
PROMPT=$(echo "$INPUT" | jq -r '.prompt // empty' 2>/dev/null)
if [ -z "$PROMPT" ]; then
    exit 0
fi

# ----- DETECT GITHUB ISSUE REFERENCES -----
ISSUE_NUM=""
REPO=""

# Pattern 1: Full GitHub URL - github.com/owner/repo/issues/123
if [[ "$PROMPT" =~ github\.com/([^/]+/[^/]+)/issues/([0-9]+) ]]; then
    REPO="${BASH_REMATCH[1]}"
    ISSUE_NUM="${BASH_REMATCH[2]}"
# Pattern 2: nutritionapp#123
elif [[ "$PROMPT" =~ (nutritionapp)#([0-9]+) ]]; then
    ISSUE_NUM="${BASH_REMATCH[2]}"
    REPO="$DEFAULT_REPO"
# Pattern 3: Simple #123 or GH-123
elif [[ "$PROMPT" =~ (^|[[:space:]])(GH-|#)([0-9]+)([[:space:]]|$) ]]; then
    ISSUE_NUM="${BASH_REMATCH[3]}"
    REPO="$DEFAULT_REPO"
fi

if [ -z "$ISSUE_NUM" ]; then
    exit 0
fi

# ----- FETCH ISSUE DETAILS (best-effort; gh may be unavailable) -----
ISSUE_DATA=$(gh issue view "$ISSUE_NUM" --repo "$REPO" --json title,body,labels,state 2>/dev/null)
if [ $? -ne 0 ] || [ -z "$ISSUE_DATA" ]; then
    exit 0
fi

TITLE=$(echo "$ISSUE_DATA" | jq -r '.title // "N/A"')
BODY=$(echo "$ISSUE_DATA" | jq -r '.body // "No description"')
STATE=$(echo "$ISSUE_DATA" | jq -r '.state // "unknown"')
LABELS=$(echo "$ISSUE_DATA" | jq -r '.labels[]?.name // empty' 2>/dev/null | tr '\n' ', ' | sed 's/, $//')

CURRENT_BRANCH=$(git branch --show-current 2>/dev/null)
HAS_CHANGES=$(git status --porcelain 2>/dev/null | head -1)
SLUG=$(echo "$TITLE" | tr '[:upper:]' '[:lower:]' | tr -cs '[:alnum:]' '-' | sed 's/^-//;s/-$//' | cut -c1-40)
SUGGESTED_BRANCH="feature/${ISSUE_NUM}-${SLUG}"

cat << EOF

## GitHub Issue Detected: #${ISSUE_NUM}

**Repository:** $REPO
**Title:** $TITLE
**State:** $STATE
**Labels:** ${LABELS:-None}

### Description:
$BODY

---

### Branch Recommendation
EOF

if [ -n "$HAS_CHANGES" ]; then
    echo "You have uncommitted changes. Commit or stash them first."
fi

cat << EOF

**Suggested workflow:**
1. \`git checkout <default-branch> && git pull\`
2. \`git checkout -b $SUGGESTED_BRANCH\`
3. Route through @mario (requirements) → @egidio (architecture) as needed.
4. Follow TDD: write tests FIRST.

---
EOF
