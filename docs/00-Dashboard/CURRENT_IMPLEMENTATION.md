# CURRENT_IMPLEMENTATION — SaigonPlanTravel

> Working-tree snapshot for 2026-08-10. Source and Flyway migrations remain
> authoritative.

## 1. Repository state

- Branch: `experiment/feat-005-codex`.
- FEAT-005A is implemented in the working tree and is not yet committed.

## 2. Implemented backend

- `auth`: registration, login, JWT authentication, and authenticated principal.
- `place`: catalog, detail, categories, weekly opening hours, search, filtering,
  and pagination.
- `trip`: authenticated create, list, get, and full replacement of owned Trip
  preferences.
- `itinerary`: authenticated get, append Place, delete item with continuous
  resequencing, and replace an item's Place for an owned Trip.
- `common`: shared error handling and infrastructure.

The itinerary is an editable ordered Place list only. There is no scheduling,
distance, travel-time, timeline, warning, routing, weather, or AI behavior.

## 3. Database

- V1–V8 establish extensions, Place data, authentication, and Trip data.
- V9 creates the one-per-Trip `itineraries` table.
- V10 creates ordered `itinerary_items` with unique Place and sequence
  constraints per itinerary.
- V11 seeds demo data.
- Hibernate remains configured with `ddl-auto: validate`.

## 4. Implemented frontend

- Place catalog/detail/search/filter/pagination and place map.
- Registration, login, and authenticated session handling.
- Trip creation, listing, detail, replacement, and origin map.
- No itinerary route, timeline, generation control, or itinerary navigation.

## 5. Verification

Verified on 2026-08-10:

- `cd backend && ./mvnw -DskipTests test` — compilation passed.
- `cd backend && ./mvnw -Dtest=ItineraryControllerTest test` — 7 tests passed.
- `cd backend && ./mvnw -Dtest=ItineraryServiceIntegrationTest test` — 8 tests
  passed against PostgreSQL 16 after all 11 migrations and Hibernate schema
  validation completed.
- `cd backend && ./mvnw test` — 94 tests passed, 0 failures, 0 errors.

Docker-backed tests were run outside the sandbox because Testcontainers requires
access to the Docker socket.
