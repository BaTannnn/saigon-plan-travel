---
id: FEAT-004
title: Authenticated Trip Preferences MVP
status: implemented
priority: P0
project: SaigonPlanTravel
owner: Nguyễn Bá Tân
updated: 2026-08-10
tags:
  - trip
  - preferences
  - authentication
  - backend
  - frontend
---

# FEAT-004 — Authenticated Trip Preferences MVP

## 1. Purpose

Authenticated users can save and manage one-day Trip preferences. A Trip is a
standalone preference record and does not imply that the system creates an
ordered plan or timeline.

## 2. Scope

- Create an owned Trip.
- List Trips owned by the authenticated user.
- Retrieve one owned Trip by public UUID.
- Fully replace one owned Trip.
- Store date, start/end time, budget, labelled origin coordinates, pace,
  environment preference, and one to five preferred category slugs.
- Resolve public category slugs to database category IDs.
- Render Trip create/list/detail/edit interfaces and the selected origin map.

## 3. Out of scope

- Selecting or ordering places.
- Producing a timeline, route, travel estimate, or generated recommendation.
- Calling external AI, weather, traffic, or route providers.
- Sharing Trips or exposing internal numeric identifiers.

## 4. API

- `POST /api/v1/trips` → `201 Created` with `Location` and `TripResponse`.
- `GET /api/v1/trips` → owned `TripSummaryResponse` list.
- `GET /api/v1/trips/{publicId}` → owned `TripResponse`.
- `PUT /api/v1/trips/{publicId}` → fully replaced `TripResponse`.

All endpoints require authentication. Missing and non-owned UUIDs share the
same not-found behavior.

## 5. Business rules

- Trip dates and time windows are validated by `TripPolicy`.
- Budget is decimal, non-negative, and bounded by the request contract.
- Origin label and coordinates are required and validated.
- Pace is `RELAXED`, `BALANCED`, or `FAST`.
- Environment preference is `INDOOR`, `OUTDOOR`, or `MIXED`.
- Category slugs are normalized, unique by resolved identity, and must refer to
  existing categories.
- Public identity is UUID; ownership comes only from the authenticated principal.

## 6. Persistence

- V11 creates `trips`.
- V12 creates `trip_category_preferences`.
- `Trip` owns its category-ID collection through JPA persistence.
- Flyway owns schema evolution and Hibernate validates mappings.

## 7. Acceptance criteria

- Valid authenticated create/list/get/replace flows succeed.
- Invalid dates, windows, coordinates, budgets, or categories return the
  established `ProblemDetail` response.
- Cross-user access does not reveal whether a Trip exists.
- Responses contain public UUIDs and DTOs, never JPA entities or internal IDs.
- Updating a Trip preserves its public identity and creation timestamp while
  changing `updatedAt`.
- Backend tests, frontend lint, and frontend build pass.

## 8. Current implementation map

- Backend: `trip/controller`, `trip/service`, `trip/domain`, `trip/entity`,
  `trip/repository`, `trip/mapper`, and `trip/dto`.
- Frontend: `app/trips/**`, `features/trips/**`, `lib/api/trip-api.ts`, and
  `types/trip.ts`.
