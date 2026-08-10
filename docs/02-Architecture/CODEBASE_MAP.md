# CODEBASE_MAP — SaigonPlanTravel

## Backend

`backend/src/main/java/com/saigonplantravel/backend/` is a Spring Boot modular
monolith organized by feature:

- `auth/`: registration, login, JWT creation/validation, security configuration,
  and authenticated user identity.
- `place/`: Place and Category persistence, opening hours, catalog/detail/search
  queries, DTO mapping, and REST controllers.
- `trip/`: owned Trip persistence, validation policy, DTO mapping, application
  service, and REST controller.
- `common/`: shared configuration and `ProblemDetail` exception handling.

### Public request flow

- Place catalog/detail: `PlaceController` → `PlaceService` → `PlaceRepository`
  → PostgreSQL → response DTO.
- Category list: `CategoryController` → `CategoryService` → repository → DTO.
- Authentication: `AuthController` → auth service/security components → users.
- Trip create/list/get/replace: `TripController` → `TripService` →
  `TripRepository`, with Category resolution through `CategoryService`.

Trip ownership always comes from `UserPrincipal`; public APIs use Trip UUIDs.

## Database

- Flyway migrations live under `backend/src/main/resources/db/migration/`.
- V1–V12 own the current extension, Place, Category, user, and Trip schema.
- V13–V15 remain historical.
- V16 removes the historical itinerary schema.
- Hibernate validates mappings rather than creating schema.

## Frontend

`frontend/src/` uses the Next.js App Router:

- `app/places/**`: public discovery and detail routes.
- `app/login` and `app/register`: authentication routes.
- `app/trips/**`: authenticated Trip preference routes.
- `features/places/`: place filters, cards, pagination, and map UI.
- `features/trips/`: Trip forms, summaries, detail, and origin map.
- `lib/api/`: typed backend API clients.
- `types/`: public API types.

Browser authentication and API calls enter through the auth provider and typed
API clients. Leaflet components remain isolated behind client-side map shells.
