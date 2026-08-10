# VIBE_SETUP_MANIFEST

## Repository guidance

- Root `AGENTS.md` defines the engineering contract and source-of-truth order.
- `backend/AGENTS.md` defines Spring, persistence, API, and test conventions.
- `frontend/AGENTS.md` defines Next.js, UI, and API-client conventions.
- `PROJECT_CONTEXT.md` records durable product scope.
- `CURRENT_IMPLEMENTATION.md` records live implementation status.
- `CODEBASE_MAP.md` maps actual packages and request flows.

## Active product baseline

- FEAT-001 through FEAT-003: public Place discovery.
- Authentication: stateless JWT registration and login.
- FEAT-004: authenticated Trip preference management.

Always inspect `git status`, current source, migrations, tests, and relevant
dirty diffs before editing.
