# PROJECT_CONTEXT — SaigonPlanTravel

> Durable project context. Current source and Flyway migrations are the primary
> source of truth; use `CURRENT_IMPLEMENTATION.md` for live status.

## 1. Product goal

SaigonPlanTravel is a mobile-first Ho Chi Minh City place-discovery and
trip-preference application. Users can browse public place data, authenticate,
and save one-day trip preferences such as date, time window, budget, origin,
pace, environment, and categories.

The product does not generate, persist, enrich, explain, assess, or dynamically
change an ordered travel timeline. Scheduling and itinerary capabilities were
permanently retired by owner decision on 2026-08-10.

## 2. Current product scope

- Public place catalog, detail, categories, opening hours, search, filters, and
  pagination.
- Stateless JWT registration and login.
- Authenticated creation, listing, retrieval, and full replacement of owned
  Trip preferences.
- Next.js interfaces for place discovery, authentication, and Trip preferences.
- `ROLE_ADMIN`-protected Thymeleaf Place administration with an all-status
  catalog, detail, basic creation, and basic editing. Public Place queries remain
  active-only. Admin can explicitly activate or deactivate a Place through
  CSRF-protected form actions and replace a Place's assignments from the existing
  Category catalog. Admin can also replace the weekly opening-hour data while
  preserving the distinction between an explicitly closed day and an unknown day.
  The admin Category catalog supports listing, creation, and editing; deletion
  is not part of the current slice.
- Leaflet/OpenStreetMap presentation for places and a Trip's selected origin.

Explicitly out of scope:

- automatic place selection or ordering;
- generated timelines or persisted itineraries;
- route-provider enrichment and travel-time computation;
- generated explanations, weather/context assessment, and dynamic plan changes;
- direct browser access to databases or third-party providers.

## 3. Technology baseline

- Backend: Java 21, Spring Boot, Spring MVC, Spring Security, Spring Data JPA,
  PostgreSQL, Flyway, and Hibernate schema validation.
- Frontend: Next.js App Router, React, TypeScript, Tailwind CSS, Radix/shadcn
  primitives, Leaflet, and React Leaflet.
- Database: Flyway is the schema-evolution source of truth.

## 4. Architecture

- Spring Boot remains a package-by-feature modular monolith.
- Current backend features are `auth`, `place`, `trip`, and shared `common`
  infrastructure.
- REST controllers return DTOs rather than JPA entities.
- Ownership is derived from the authenticated principal.
- Public Trip identity is UUID; internal numeric IDs remain internal.
- Frontend calls only the Spring backend contract.

## 5. Stable domain semantics

### Place

- Place data is scoped to the Ho Chi Minh City project dataset.
- Category and weekly opening-hour metadata support discovery and detail views.
- Missing opening-hour data remains unknown and must not be silently presented
  as closed.
- Demo or unverified facts remain labelled accordingly.

### Trip

- A Trip is an authenticated user's saved one-day preference record.
- It includes date, start/end time, budget, labelled origin coordinates, pace,
  environment preference, and one to five preferred categories.
- A Trip does not imply that an ordered plan or timeline exists.
- Users can create, list, retrieve, and fully replace only their own Trips.

## 6. Security and evidence

- REST authentication uses stateless JWT Bearer tokens. Thymeleaf admin pages
  under `/admin/**` use session-backed form authentication, require `ROLE_ADMIN`,
  and retain CSRF protection.
- Credentials and secrets must not be persisted in source or logs.
- Public errors use the project's `ProblemDetail` conventions.
- Demo data must not be presented as verified real-world fact.
- Test, performance, and quality claims require measured evidence.

## 7. Feature baseline

- FEAT-001: place catalog.
- FEAT-002: place detail, categories, and opening hours.
- FEAT-003: place search, filtering, and pagination.
- FEAT-004: authenticated Trip preferences.

No later numbered feature is currently defined.

## 8. Documentation roles

- `PROJECT_CONTEXT.md`: durable scope and invariants.
- `CURRENT_IMPLEMENTATION.md`: live implementation and verification state.
- `CODEBASE_MAP.md`: concrete packages, routes, and data flow.
- Feature specifications: accepted behavior of active features.
- Development logs: historical evidence for retained milestones.
