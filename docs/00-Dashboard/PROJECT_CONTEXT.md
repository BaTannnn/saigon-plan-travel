# PROJECT_CONTEXT — SaigonPlanTravel

> Durable project context for ChatGPT/Codex.
> For live implementation status, test evidence, migration head, and current work,
> read `docs/00-Dashboard/CURRENT_IMPLEMENTATION.md`.
> For concrete package/class/request flows, read `docs/02-Architecture/CODEBASE_MAP.md`.
> Current source code and Flyway migrations remain authoritative when documents drift.

## 1. Project goal

SaigonPlanTravel is a mobile-first web system for intelligent travel itinerary
planning in Ho Chi Minh City.

The graduation-project framing is a **constrained travel scheduling problem**, not
merely a travel website with AI. The system combines:

1. place discovery and preference matching;
2. deterministic one-day itinerary scheduling;
3. grounded RAG for place/itinerary explanation;
4. time-indexed context such as weather, traffic, or crowd-related signals;
5. controlled dynamic re-planning when relevant context changes.

The MVP targets roughly 30–100 places in Ho Chi Minh City. Complete a defendable MVP
before considering AR/Unity, native mobile apps, microservices, complex deep learning,
global optimization, or unrelated platform capabilities.

## 2. Product scope

### Core MVP direction

A user should be able to:

- discover places in Ho Chi Minh City;
- authenticate;
- create travel preferences including date, time window, budget, origin, pace,
  environment preference, and categories;
- generate a deterministic itinerary;
- review a persisted timeline, estimated cost, estimated travel time/distance,
  assumptions, and warnings;
- later enrich the itinerary with route, grounded explanation, time-based context,
  and controlled re-planning through their dedicated features.

### Out of scope for the current MVP baseline

Unless an approved feature explicitly changes scope, do not introduce:

- multi-day or overnight scheduling;
- autonomous LLM itinerary generation;
- global TSP/VRP optimization;
- microservices solely for architectural appearance;
- Redis/Kafka/message queues without a current requirement;
- native mobile or AR/Unity;
- deep-learning forecasting as a prerequisite for context-aware scheduling.

## 3. Technology baseline

Actual versions in build/package files are authoritative.

### Backend

- Java 21.
- Spring Boot.
- Spring MVC.
- Spring Security with stateless JWT authentication.
- Spring Data JPA/Hibernate.
- PostgreSQL.
- Flyway migration-first schema management.
- Hibernate schema validation with `ddl-auto=validate`.
- PostgreSQL/Testcontainers integration testing where database behavior matters.

### Frontend

- Next.js App Router.
- React.
- TypeScript.
- Tailwind CSS.
- shadcn/Radix UI primitives.
- Leaflet + React Leaflet for map UI.
- Mobile-first Mint Map visual direction.

### AI/context direction

- Python FastAPI is the planned AI-service boundary for RAG.
- LangChain may be used inside the RAG service when it materially simplifies the pipeline.
- PostgreSQL/pgvector is the preferred vector-search baseline for the MVP.
- Route, RAG, weather/context, and re-planning integrations belong to their dedicated
  feature contracts rather than the deterministic FEAT-005 scheduler.

## 4. Architectural principles

- Backend remains a Spring Boot modular monolith organized package-by-feature.
- PostgreSQL is the business persistence source of truth.
- Flyway owns schema evolution; Hibernate validates mappings only.
- Public REST controllers return DTOs, never JPA entities.
- Cross-module access uses explicit service/query/read-model contracts instead of
  importing another module's repository or entity for convenience.
- Scheduling domain logic remains deterministic and independent from Spring, JPA,
  HTTP, Clock, route providers, weather providers, RAG, and LLMs when practical.
- RAG explains already-decided business results; it does not silently replace the
  deterministic itinerary-selection baseline.
- Route/weather/context integrations enrich or assess an immutable itinerary baseline;
  they must not silently rewrite it.
- Avoid abstractions, frameworks, or infrastructure added only for hypothetical future use.

## 5. Stable domain semantics

### Place

- Places belong to the Ho Chi Minh City project dataset.
- Category metadata expresses user interests.
- Missing weekly opening-hours data means `UNKNOWN`, not `CLOSED`.
- Administrative-unit name/type may be absent; the system must not infer missing
  administrative data silently from an address or coordinates.
- Demo/unverified place descriptions, costs, addresses, and hours must remain labeled
  as demo/unverified data until provenance is established.

### Trip

- The current planning baseline is an authenticated, one-day Trip.
- Trip preferences include date, start/end time, budget, origin, travel pace,
  environment preference, and preferred categories.
- Public Trip identity uses UUID; ownership comes from the authenticated principal.
- Scheduling reads Trip through an immutable scheduling contract/snapshot rather than
  importing the Trip persistence model.

### Scheduling

- Candidate places are read through the Place scheduling contract.
- Preferred categories use OR eligibility; matching more preferred categories may
  improve score.
- Environment preference supports indoor, outdoor, or mixed eligibility.
- Budget feasibility uses the place minimum-cost snapshot and decimal money values.
- Known closed/closing-time conflicts are hard constraints.
- Missing opening-hours data may remain eligible with explicit warning semantics.
- Haversine represents straight-line distance, not road-route distance.
- Baseline travel minutes are estimates derived from configured assumptions.
- Remaining candidates must be re-evaluated after every selection because location,
  time, waiting, and remaining budget change.
- Selection is deterministic for the same immutable inputs/configuration.
- A selected place cannot repeat within the generated itinerary.
- The deterministic scheduler does not call route, weather, RAG, LLM, or AI services.

## 6. Security and public API principles

- Authentication uses stateless JWT Bearer tokens.
- Passwords are stored using the backend's password encoder; plaintext passwords must
  never be persisted or logged.
- Protected Trip/Itinerary ownership is derived from the authenticated user, not from
  client-supplied user IDs.
- Public Trip/Itinerary resources use UUID identifiers; internal numeric IDs stay internal.
- Browser-to-backend calls may use the configured explicit CORS origin. Do not replace
  explicit origins with wildcard CORS for convenience.
- Current JWT authentication uses the `Authorization` header, not credentialed browser
  cookies; any future cookie-based auth change requires an explicit security decision.
- `.env`, JWT secrets, database credentials, provider keys, and access tokens must not
  be committed or logged.
- Public errors use the project's `ProblemDetail` conventions and must not expose SQL,
  stack traces, tokens, or provider secrets.

## 7. Frontend principles

- Preserve the current Mint Map visual direction and mobile-first layout.
- Prefer Server Components for public/read-only composition when browser state is not needed.
- Use Client Components only for interaction, browser-only state/APIs, authenticated
  browser requests, and Leaflet-specific behavior.
- Do not duplicate backend scheduling rules in TypeScript.
- Backend validation remains authoritative even when frontend validation improves UX.
- Reuse existing shadcn/Radix primitives before creating new generic UI abstractions.
- Keep feature logic under feature-oriented folders; generic UI primitives must not
  become a place for business logic.
- Do not call OSRM, Open-Meteo, LLM providers, or the database directly from the browser
  when the corresponding feature assigns ownership to Spring/FastAPI.

## 8. Feature direction

Use `CURRENT_IMPLEMENTATION.md` for actual completion status. The intended sequence is:

- FEAT-001–003: place catalog/detail/search foundation;
- Authentication: prerequisite for owned Trip/Itinerary flows;
- FEAT-004: authenticated Trip preferences;
- FEAT-005: deterministic itinerary generation and persisted timeline;
- FEAT-006: route/map enrichment without changing FEAT-005 order;
- FEAT-007: grounded RAG explanation without deciding the itinerary;
- FEAT-008: time-indexed weather/context assessment;
- FEAT-009: controlled dynamic re-planning proposals.

Feature documents may contain older terminology or stale status. Source plus
`CURRENT_IMPLEMENTATION.md` determine what is actually implemented.

## 9. Evidence and thesis rules

- Do not present simulated/demo data as verified real-world data.
- Haversine must be described as straight-line distance, not real route distance.
- Estimated travel time and minimum-cost snapshots must be labeled as estimates.
- Forecast data must be distinguished from observations or real-time measurements.
- RAG claims must distinguish retrieved evidence from generated explanation text.
- Do not claim latency, accuracy, coverage, optimization quality, or test success without
  measured evidence.
- Keep enough snapshots/configuration/evaluation evidence to reproduce thesis results.

## 10. Documentation roles

Do not use one document for every type of context.

- `PROJECT_CONTEXT.md` — durable goals, scope, architectural invariants, terminology.
- `CURRENT_IMPLEMENTATION.md` — live feature status, current migration/test evidence,
  temporary implementation facts.
- `CODEBASE_MAP.md` — actual package/class/module/request/data flows.
- `docs/01-Requirements/Features/` — feature-specific business/API/schema/acceptance rules.
- `docs/05-Testing-and-Learning/` — owner learning/workflow guides.
- `docs/09-Development-Log/` — milestone history and implementation evidence.

When these disagree, follow the source-of-truth order defined in root `AGENTS.md`.
