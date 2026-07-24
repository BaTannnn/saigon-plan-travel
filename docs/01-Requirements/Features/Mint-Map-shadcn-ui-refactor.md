---
title: Mint Map shadcn/ui Frontend Refactor
status: approved
project: SaigonPlanTravel
owner: Nguyễn Bá Tân
created: 2026-07-21
updated: 2026-07-21
tags:
  - frontend
  - refactor
  - shadcn-ui
  - accessibility
related:
  - "[[00-Dashboard/PROJECT_CONTEXT]]"
  - "[[01-Requirements/Features/FEAT-002-place-detail-category-opening-hours]]"
  - "[[01-Requirements/Features/FEAT-003-place-search-filter-pagination]]"
---

# Mint Map shadcn/ui Frontend Refactor

## Locked requirements

- Refactor only the existing frontend UI primitives; this is not a redesign or
  a new product feature.
- Keep the current Mint Map layout, colors, spacing, typography, responsive
  behavior and reference image `docs/05-UI-UX/option-2-mint-map.png`.
- Keep `/places`, `/places/[slug]`, the backend endpoints, query parameters,
  zero-based pagination, URL-owned filter state and API DTOs unchanged.
- Keep server-side data fetching and Server Components as the default. Add a
  Client Component boundary only where state, events, browser APIs, Radix or
  Leaflet require it.
- Initialize shadcn/ui in the current Next.js application with npm, the existing
  `@/*` alias and Tailwind CSS 4. Do not scaffold another application or upgrade
  Next.js, React or Tailwind.
- Add only primitives used by the implementation. `components/ui` must contain
  reusable UI primitives, not API calls or place business behavior.
- Centralize Mint Map colors in semantic CSS variables. Do not add dark mode or
  external fonts.
- Preserve all existing user changes in the dirty worktree. Do not commit or
  push Git changes.

## Out of scope

- Backend, database, Flyway, API contract or TypeScript DTO changes.
- Authentication, a functional user menu or the disabled itinerary action.
- FEAT-004–009, mock/fallback place data, a new state-management library or a
  new test framework.
- Replacing the business-specific list/map layout with shadcn Sidebar.
- Rewriting all of `globals.css` or replacing every native/semantic element.

## Primitive mapping

| Existing UI | Refactor target |
| --- | --- |
| Repeated buttons and button-like links | `Button` with Mint Map variants |
| Search and budget fields | `Input` with `Label`/accessible name |
| District, category and indoor controls | `Select` |
| Mobile filter backdrop/dialog | `Sheet` |
| Loading placeholders | `Skeleton` |
| Error messages | `Alert` |
| User initial | `Avatar` |
| Repeated place/detail surfaces | `Card` where it reduces repeated styles |
| Previous/next navigation | `Pagination` primitives |

Category chips remain URL-driven `Link` elements because navigation is their
semantic behavior. `PlacesExplorer`, `PlaceCard`, `PlaceMap`, `MapShell`, map
markers, filter/search logic and marker/card synchronization remain business
components even when they consume a UI primitive.

## Business rules and edge cases

- Any filter change resets `page` to 0; pagination continues to display
  one-based labels while sending backend page indexes unchanged.
- `indoor=false` remains a present filter value.
- A valid empty response shows the existing empty state; an unavailable backend
  never falls back to fake data; detail 404 keeps its dedicated state.
- Select and Sheet must support keyboard use, focus management and accessible
  labels. Icon-only buttons require an accessible name and mobile controls keep
  an approximately 44 px target.
- Leaflet remains dynamically imported with `ssr: false`; OpenStreetMap and the
  marker/card selection behavior do not change.
- Production build must not require the backend to be running.

## Acceptance criteria

- [ ] shadcn/ui configuration and only the used primitives exist in frontend.
- [ ] Mint Map semantic tokens match the approved palette.
- [ ] `/places` retains search, filters, reset, pagination, list/map switching
      and marker/card synchronization on desktop and mobile.
- [ ] `/places/[slug]` retains detail fields, opening-hour semantics and map.
- [ ] Loading, empty, error and not-found behavior remain available.
- [ ] No backend/API/type contract files are changed.
- [ ] TypeScript strict checks contain no `any` introduced by the refactor.
- [ ] Formatter (if configured), lint, type check, existing tests and production
      build are run and results recorded.
- [ ] Learning documentation explains shadcn/ui, `components/ui`, `cn()`, Button
      variants, client boundaries and business components versus primitives.

## Open risks

- Base primitive styles can shift visual dimensions unless business-level Mint
  Map classes override them deliberately.
- Radix Select must contribute the correct `name` and value to the GET form.
- Sheet portal stacking must remain above Leaflet controls without changing map
  interaction.
- Browser screenshot automation may be unavailable in the execution
  environment; any unverified visual state must be reported explicitly.

## Verification

```bash
cd frontend
npm run lint
npx tsc --noEmit
npm run build
```

The repository currently has no formatter script or frontend test script. Do
not add a testing framework solely for this refactor.
