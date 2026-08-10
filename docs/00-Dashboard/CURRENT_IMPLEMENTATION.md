# CURRENT_IMPLEMENTATION — SaigonPlanTravel

> Working-tree snapshot for 2026-08-10. Source and Flyway migrations remain
> authoritative.

## 1. Repository state

- Branch: `experiment/feat-005-codex`.
- The worktree contains uncommitted owner changes in backend Place code and
  frontend Trip presentation.
- The approved removal preserves unrelated Place/Trip/frontend work while
  deleting the retired capability.

## 2. Implemented backend

- `auth`: registration, login, JWT authentication, and authenticated principal.
- `place`: catalog, detail, categories, weekly opening hours, search, filtering,
  and pagination.
- `trip`: authenticated create, list, get, and full replacement of owned Trip
  preferences.
- `common`: shared error handling and infrastructure.

There is no active scheduling module or itinerary API.

## 3. Database

- V1–V12 establish extensions, Place data, authentication, and Trip data.
- V13–V15 are retained as immutable historical migrations.
- V16 removes the historical itinerary tables and their data.
- Hibernate remains configured with `ddl-auto: validate`.

## 4. Implemented frontend

- Place catalog/detail/search/filter/pagination and place map.
- Registration, login, and authenticated session handling.
- Trip creation, listing, detail, replacement, and origin map.
- No itinerary route, timeline, generation control, or itinerary navigation.

## 5. Verification

Verified on 2026-08-10:

- `cd backend && ./mvnw -q -DskipTests compile` — passed.
- `cd backend && ./mvnw -Dtest=FlywayMigrationTest test` — 3 tests passed;
  V1–V16 migrated on clean PostgreSQL, Hibernate validation passed, and the
  four historical itinerary tables were absent.
- `cd backend && ./mvnw test` — 79 tests passed, 0 failures, 0 errors.
- `cd frontend && npm run lint` — passed with no lint errors.
- `cd frontend && npm run build` — passed; all current Place/Auth/Trip routes
  compiled and generated successfully.

Docker-backed backend tests and the frontend build were run outside the sandbox
because Testcontainers requires the Docker socket and Next.js required external
font access.
