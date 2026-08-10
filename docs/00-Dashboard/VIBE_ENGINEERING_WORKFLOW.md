# Vibe Engineering Workflow

## Goal

Use Codex to accelerate small, defendable project slices while preserving owner
understanding and uncommitted work.

## Before implementation

1. Inspect branch and dirty worktree.
2. Read `AGENTS.md`, project context, current implementation, and the relevant
   active feature spec.
3. Inspect exact source, migration, and tests.
4. Use requirements discovery only for unsettled behavior.
5. Lock scope and approval before destructive or architectural changes.

## During implementation

- Work on one coherent behavior.
- Preserve unrelated dirty changes.
- Do not rewrite applied migrations.
- Keep controllers thin and persistence migration-first.
- Avoid speculative abstractions and dependencies.
- Add the smallest tests that prove business boundaries and regressions.

Example bounded requests:

- “Add one approved Place filter and its repository/API tests.”
- “Connect the existing Trip list endpoint to the Trip hub page.”
- “Review the current Trip replacement diff and explain its data flow.”

## Verification

- Backend checkpoint: `cd backend && ./mvnw test`.
- Frontend checkpoint: `cd frontend && npm run lint && npm run build`.
- Release checkpoint: `cd backend && ./mvnw clean verify`.

Always report actual commands and results. Never turn a failed check into a
success claim.

## Completion report

Explain result, changed files, execution flow, key concepts, verification,
diff audit, manual inspection steps, and exactly one recommended next slice.
