---
name: project-context-audit
description: Read the SaigonPlanTravel repo, git state, current implementation docs, relevant specs, migrations, source, and tests to recover accurate context before coding. Use when starting a new thread/module, returning after a break, asking “where are we/what next?”, or when docs and source may disagree. Read-only; do not implement production code.
---

# Project Context Audit

Recover the current source truth without changing production code.

## Preflight

1. Run `git status --short` and `git branch --show-current`.
2. Read root `AGENTS.md` and the nearest nested `AGENTS.md`.
3. Read:
   - `docs/00-Dashboard/PROJECT_CONTEXT.md`
   - `docs/00-Dashboard/CURRENT_IMPLEMENTATION.md`
   - `docs/02-Architecture/CODEBASE_MAP.md` when architecture is relevant.
4. Read the relevant FEAT spec.
5. Inspect the actual source, migrations, tests, and current diff for the target area.

Treat source/migrations as stronger evidence than stale docs.

## Audit rules

- Do not edit production code.
- Do not reset/stash/restore dirty work.
- Do not infer implementation from a feature status label alone.
- Identify contradictory docs explicitly.
- Do not propose a large refactor unless a concrete current problem requires it.
- If a user question can be answered from source, answer it rather than asking the user.

## Required output

Return these sections:

1. `CURRENT STATE`
2. `DIRTY WORKTREE / PRESERVE`
3. `SOURCE FLOW`
4. `CONTRACTS AND BOUNDARIES`
5. `DOC/SOURCE CONFLICTS`
6. `RISKS OR BLOCKERS`
7. `SMALLEST USEFUL NEXT SLICE`
8. `FILES TO READ BEFORE THAT SLICE`
9. `VERIFICATION COMMANDS`

Keep the audit concise and source-specific. Stop after recommending one next slice.
