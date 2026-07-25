---
description: Lightweight curation pass on the project's auto-memory store. Scans the Claude memory directory for stale entries, closed-issue references, and broken file paths. Suggests archive candidates; the user decides what to remove. Suggested cadence — once a month.
allowed-tools: [Read, Grep, Glob, Bash]
---

Audit the Meridia memory store and report stale entries.

## What to do

1. Locate the memory directory (Claude Code stores per-project memory under
   `~/.claude/projects/<slugified-project-path>/memory/`). List it:
   ```bash
   MEM_DIR="$HOME/.claude/projects/$(pwd | sed 's|/|-|g')/memory"
   ls -la "$MEM_DIR"/*.md 2>/dev/null || echo "No memory directory yet."
   ```

2. **For each `.md` file except `MEMORY.md`**, run these checks:

   **a) Age + reference check.** Candidate for archive if BOTH:
   - `mtime` older than 90 days (`find "$MEM_DIR" -name "*.md" -mtime +90`)
   - No other memory file or `CLAUDE.md` links to it (grep the slug across `memory/` and `CLAUDE.md`)

   **b) Closed-issue check.** If the body cites a GitHub issue (`#\d+`), check whether it is CLOSED. Flag entries whose only referenced issue is closed.

   **c) Broken file paths.** Grep each memory for file paths (`[a-zA-Z_./-]+\.(py|kt|kts|md|yaml|yml|json|sh)`). Flag entries referencing files that no longer exist.

   **d) Duplicate detection.** Flag pairs of memories with high word-set overlap as merge candidates.

3. **Report format:**
   ```
   ## Memory Audit Report — <today>
   **Total memories:** N   **Candidates for review:** M
   ### Stale (>90d, unreferenced)
   ### Closed-issue references
   ### Broken file paths
   ### Potential duplicates
   ## Action items — per flagged memory, offer: Archive / Update / Keep
   ```

4. **Do not modify any memory files** without the user's explicit per-item approval. Memory edits are user-driven.

## Why not a hook

A periodic LLM-driven scan in a hook would fire mid-flow when the user wants focus. Hooks are reserved for enforcement, not periodic housekeeping. Memory hygiene is not time-sensitive, so user control is preferred here.
