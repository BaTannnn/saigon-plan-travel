# AGENTS.md — SaigonPlanTravel Engineering Contract

## Mission

Build a defendable graduation-project MVP for Ho Chi Minh City place discovery
and authenticated trip-preference management.

The owner uses Codex to accelerate engineering, but must still be able to explain
architecture, data flow, important algorithms, and every material code change.
Optimize for **working MVP + understanding**, not maximum abstraction.

## Source-of-truth order

When files disagree, use this order:

1. Current working-tree source code and Flyway migrations.
2. Tests that exercise the current source.
3. `docs/00-Dashboard/CURRENT_IMPLEMENTATION.md`.
4. `docs/00-Dashboard/PROJECT_CONTEXT.md`.
5. Relevant feature specification under `docs/01-Requirements/Features/`.
6. Older architecture/API/database/development notes.

Never silently “correct” current code to match an older document. Report the
conflict first and state which source is newer.

## Mandatory context before code

Before planning or editing production code:

1. Run `git status --short` and `git branch --show-current`.
2. Read this file and the nearest nested `AGENTS.md`.
3. Read:
   - `docs/00-Dashboard/PROJECT_CONTEXT.md`
   - `docs/00-Dashboard/CURRENT_IMPLEMENTATION.md`
4. Read the relevant feature spec.
5. Inspect the exact source, migration, and tests involved in the task.

Do not ask the owner to repeat information that can be read safely from the repo.

## Work modes

### 1. Existing requirement / continuation

When the requested work is already specified and the current implementation is
clear, **do not restart requirements discovery**. Use `$implement-approved-slice`.

Examples:

- continue the next approved Trip persistence step;
- fix one failing test whose intended behavior is already known;
- connect an already-defined backend endpoint to the frontend;
- implement a small part of an approved feature contract.

### 2. New or ambiguous behavior

Use `$grill-with-docs` only when a task introduces or changes material product
behavior, architecture, database ownership, public API semantics, external
provider behavior, recommendation policy, or other product semantics that are
not already settled.

Do not grill the owner for tiny fixes or for decisions already present in source/docs.

### 3. Review / learning

Use `$review-and-teach` when the owner wants to understand, defend, or review a
diff. Review is read-only unless the owner explicitly asks for fixes.

### 4. Thesis synchronization

Use `$thesis-sync` after a coherent feature/milestone is implemented and verified.
Do not write thesis claims that are not supported by source, tests, or measured evidence.

## Vibe-engineering guardrails

- Work on one coherent slice at a time. A slice may span several files when they
  form one behavior, but do not implement an entire large feature in one turn.
- If a task appears to require broad changes across unrelated modules, stop and
  propose a split before editing.
- Never make unrelated refactors “while here”.
- Never add a production dependency, framework, database, queue, cache, or new
  external service without explicit owner approval.
- Do not introduce an abstraction only because it may be useful later. Extract
  when there is a current responsibility, repeated logic, or a clear boundary.
- Prefer private methods and existing types over new factories/managers/policies
  when a small local solution remains clear.
- Prefer the smallest useful test set. Test business rules, boundaries, contracts,
  and regressions; do not create tests for trivial getters/records just for count.
- Mock repositories/external boundaries. Prefer real immutable records/value
  objects instead of mocking them.

## Dirty-worktree safety

The owner often works with uncommitted learning changes.

- Never run `git reset --hard`, `git checkout -- <file>`, `git restore`, `git clean`,
  `git stash`, or destructive history commands unless explicitly requested.
- Never overwrite a dirty file blindly.
- If a dirty file is directly in scope, inspect the diff first and preserve the
  owner's changes while extending them.
- If a dirty file is unrelated, leave it untouched.
- Report pre-existing modifications separately from changes made in the current task.

## Architecture constraints

- Backend: Spring Boot modular monolith, package-by-feature.
- Frontend: Next.js + TypeScript, mobile-first.
- Database: PostgreSQL; Flyway is migration source of truth.
- Hibernate: `ddl-auto: validate`.
- Map baseline: Leaflet/OpenStreetMap for place and Trip-origin presentation.
- REST controllers return DTOs, never JPA entities.
- Constructor injection by default.
- Do not leak internal numeric Trip IDs through public API unless the
  existing contract explicitly allows the field.
- Never commit `.env`, credentials, tokens, API keys, or provider secrets.
- Never present demo/unverified place data as verified fact.

## Verification discipline

Do not claim success without naming the commands actually run and their results.
If the environment prevents a command, say exactly why and provide the local command.

Backend baseline:

```bash
cd backend
./mvnw test
```

Focused backend work may run targeted tests first, then the broader suite when feasible.
Before release/integration checkpoint:

```bash
cd backend
./mvnw clean verify
```

Frontend baseline:

```bash
cd frontend
npm run lint
npm run build
```

Never hide a failing test by weakening production behavior or deleting a meaningful
assertion without explaining the intended contract.

## Required completion report after implementation

After a coding slice, report in this order:

1. **Result** — what behavior now exists.
2. **Changed files** — every file changed and why.
3. **Execution flow** — request/domain/data flow through the actual code.
4. **Key concepts** — Java/Spring/JPA/Next.js/algorithm concepts the owner should know.
5. **Verification** — exact commands and real results.
6. **Diff audit** — unrelated edits, overengineering risk, dependency/migration impact.
7. **How to inspect manually** — 2–5 concrete places/commands the owner can check.
8. **One recommended next slice** — do not automatically implement it.

The explanation should be concise enough to read, but complete enough that the owner
can explain the change in a thesis defense or interview.

## Documentation policy

Update durable docs when behavior or architecture actually changes. Do not generate
new documentation for every tiny refactor.

Important locations:

- Current status: `docs/00-Dashboard/CURRENT_IMPLEMENTATION.md`
- Durable project context: `docs/00-Dashboard/PROJECT_CONTEXT.md`
- Codebase learning map: `docs/02-Architecture/CODEBASE_MAP.md`
- Feature requirements: `docs/01-Requirements/Features/`
- ADRs: `docs/02-Architecture/ADR/`
- Database notes: `docs/03-Database/`
- API notes: `docs/04-API/`
- Learning notes: `docs/05-Testing-and-Learning/`
- Development log: `docs/09-Development-Log/`

Create an ADR only for a costly/difficult-to-reverse decision. Prefer updating an
existing note over creating near-duplicates.
