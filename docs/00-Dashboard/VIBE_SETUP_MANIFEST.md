# VIBE_SETUP_MANIFEST — 2026-08-09

This setup changes only repository instructions/documentation and skill files. It does
not intentionally modify application production source.

## Files added

- `AGENTS.md`
- `.agents/skills/grill-with-docs/**`
- `.agents/skills/project-context-audit/**`
- `.agents/skills/implement-approved-slice/**`
- `.agents/skills/review-and-teach/**`
- `.agents/skills/thesis-sync/**`
- `docs/00-Dashboard/CURRENT_IMPLEMENTATION.md`
- `docs/00-Dashboard/VIBE_ENGINEERING_WORKFLOW.md`
- `docs/00-Dashboard/VIBE_SETUP_MANIFEST.md`
- `docs/02-Architecture/CODEBASE_MAP.md`

## Files updated

- `.gitignore` — root `AGENTS.md` is no longer ignored so project instructions can be version-controlled.
- `backend/AGENTS.md` — current Spring/module/scheduling/testing rules.
- `frontend/AGENTS.md` — current Next.js/frontend boundaries and verification rules.
- `docs/00-Dashboard/PROJECT_CONTEXT.md` — synchronized with the uploaded source through FEAT-005 D7/E1 progress.

## Production code intentionally preserved

The uploaded repository already had an uncommitted modification to:

- `backend/src/main/java/com/saigonplantravel/backend/scheduling/entity/Itinerary.java`

This setup does not replace or revert that file. It remains the owner's current FEAT-005
E1 work.

## First local verification

After copying the setup into the real repository:

```bash
git status --short
```

Confirm the only expected changes are the setup files plus your existing application work.
Then start a fresh Codex session from the repository root and ask:

```text
Summarize the active AGENTS.md instructions and list the repo skills available for SaigonPlanTravel.
Do not modify files.
```

Then verify locally:

```bash
cd backend
./mvnw test
```

```bash
cd ../frontend
npm run lint
npm run build
```

The setup audit environment could not download Maven Wrapper dependencies, so it does not
claim those checks pass here.
