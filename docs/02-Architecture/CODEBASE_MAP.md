# CODEBASE_MAP — SaigonPlanTravel

## Backend

`backend/src/main/java/com/saigonplantravel/backend/` is a Spring Boot modular
monolith organized by feature:

- `auth/`: registration, login, JWT creation/validation, security configuration,
  and authenticated user identity.
- `admin/`: session-authenticated Thymeleaf page controllers. `/admin/**` is
  isolated from the stateless REST chain and requires `ROLE_ADMIN` except for
  `/admin/login`.
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
- Admin authentication: `/admin/login` → database-backed `UserDetailsService`
  → BCrypt verification → HTTP session; protected admin pages require
  `ROLE_ADMIN` and CSRF-protected form submissions.
- Admin Place list: `AdminPlaceController` →
  `PlaceService.getPlacesForAdministration` → all-status Place query → admin
  response DTO with `active` → Thymeleaf table. Public REST search remains
  active-only.
- Admin Place detail: `/admin/places/{slug}` → `AdminPlaceController` →
  `PlaceService.getPlaceDetailForAdministrationBySlug` → active or inactive
  Place, categories, and opening hours → admin detail DTO → Thymeleaf page.
- Admin basic Place creation: `GET /admin/places/new` renders a CSRF-protected
  form; `POST /admin/places` validates and normalizes `PlaceCreateRequest` →
  `PlaceService.createPlace` transaction → active `Place` insert → redirect to
  `/admin/places/{slug}`. Categories and opening hours are not created here.
- Admin basic Place editing: `GET /admin/places/{slug}/edit` loads either status;
  `POST /admin/places/{slug}` validates and normalizes `PlaceUpdateRequest` →
  transactional entity mutation → redirect to detail. Slug, active status,
  categories, and opening hours remain unchanged by this form.
- Admin Place status changes: CSRF-protected POST actions at
  `/admin/places/{slug}/activate` and `/admin/places/{slug}/deactivate` →
  transactional `Place.activate`/`Place.deactivate` mutation → redirect to the
  admin detail page. Public Place queries immediately reflect the active flag.
- Admin Place category assignment: `GET /admin/places/{slug}/categories/edit`
  renders the existing Category catalog and current selections; CSRF-protected
  `POST` to the same route validates the submitted slugs → resolves every
  Category from PostgreSQL → transactionally replaces the `place_categories`
  rows. Unknown slugs are rejected, while an empty selection removes all
  assignments. Category creation remains outside this flow.
- Admin Place opening hours: `GET /admin/places/{slug}/opening-hours/edit`
  renders all seven ISO weekdays with `UNKNOWN`, `CLOSED`, or `OPEN` state;
  CSRF-protected `POST` validates the complete week and open/close range →
  `PlaceService.replacePlaceOpeningHours` → aggregate mutations on `Place` and
  `OpeningHour` → Hibernate updates, inserts, or orphan-removes `opening_hours`
  rows. `UNKNOWN` means no row, not a closed row.
- Admin Category catalog: `GET /admin/categories` loads the description-aware
  admin catalog through `CategoryService`; `GET /admin/categories/new` renders a
  CSRF-protected form; `POST /admin/categories` validates and normalizes name,
  slug, and description → checks current unique name/slug → inserts and flushes
  the Category → redirects to its catalog anchor. `GET /admin/categories/{slug}/edit`
  and `POST /admin/categories/{slug}` update name and description while keeping
  slug immutable. The public Category DTO and `/api/v1/categories` contract
  remain unchanged.
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
