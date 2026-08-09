<!-- BEGIN:nextjs-agent-rules -->
# Local Next.js version is authoritative

The project uses a recent Next.js version whose APIs may differ from model memory.
Before changing framework-sensitive code, read the relevant local documentation in
`node_modules/next/dist/docs/` when dependencies are installed. Heed deprecations.
<!-- END:nextjs-agent-rules -->

# frontend/AGENTS.md — Next.js Rules

These instructions extend the repository root `AGENTS.md`.

## Actual frontend baseline

- Next.js 16.2.x
- React 19.2.x
- TypeScript 5
- Tailwind CSS 4
- shadcn/Radix UI primitives
- Leaflet + React Leaflet
- Mobile-first interface

Current implemented user-facing slice is primarily Place discovery/detail/map.
Authentication, Trip, itinerary generation, RAG explanation, weather/context, and
replanning UIs must be added only when their backend contracts are ready.

## Structure

Prefer feature-oriented code:

```text
src/
├── app/                 route composition
├── features/<feature>/  feature components/state/helpers
├── components/ui/       reusable visual primitives
├── components/layout/   shared layout
├── lib/api/             project API clients
└── types/               shared API types when genuinely shared
```

Do not put feature business logic into generic `components/ui`.

## Rendering boundaries

- Prefer Server Components when browser state/effects are not needed.
- Add `"use client"` only where state, events, browser APIs, or client-only libraries require it.
- Leaflet/map components are client-side concerns; isolate them instead of turning an
  entire route tree into client components.
- Keep page files focused on routing/data composition; feature rendering belongs in
  feature components.

## API ownership

- Frontend calls SaigonPlanTravel backend contracts, not PostgreSQL or internal services.
- Do not call OSRM/Open-Meteo/LLM providers directly from browser code when the feature
  specification assigns the integration to Spring/FastAPI.
- Do not duplicate scheduling feasibility/scoring rules in TypeScript.
- UI validation may improve UX but backend validation remains authoritative.
- Keep auth tokens/credentials out of source and logs.

## UI and styling

- Reuse existing shadcn/Radix primitives before inventing another design system.
- Prefer Tailwind utility classes and small feature CSS only where the map/library needs it.
- Do not grow `globals.css` into a feature stylesheet.
- Preserve the Mint Map visual direction already present in the project unless the owner
  explicitly asks for a redesign.
- Keep mobile-first behavior and accessible loading/error/empty states.

## Type safety

- Keep API response/request types aligned with the backend contract.
- Avoid `any` for network/domain payloads.
- Normalize URL/search-param state in dedicated helpers instead of scattering parsing
  across components.
- Do not silently invent backend fields that do not exist.

## Verification

After frontend changes, run the smallest relevant check and then, at a checkpoint:

```bash
npm run lint
npm run build
```

If a build cannot run because dependencies or network are unavailable, report that
explicitly instead of claiming success.

## Completion explanation

For frontend changes, explain:

1. route/component flow;
2. server vs client component boundary;
3. where API data enters;
4. state/search-param behavior;
5. reusable UI primitives used;
6. lint/build result;
7. any backend contract dependency still missing.
