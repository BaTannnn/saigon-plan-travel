# AGENTS.md — SaigonPlanTravel

## Mission

Build SaigonPlanTravel, a mobile-first intelligent travel itinerary planning system for Ho Chi Minh City using RAG, time-based context analysis, and dynamic replanning.

## Read before working

Before planning or editing code, read:

1. `docs/00-Dashboard/PROJECT_CONTEXT.md`
2. The nearest more-specific `AGENTS.md`
3. Relevant feature spec under `docs/01-Requirements/Features/`
4. Relevant ADR, database, API, and development notes

Treat `PROJECT_CONTEXT.md` as the source of truth for scope and current state.

## Repository layout

- `backend/`: Spring Boot modular monolith
- `frontend/`: Next.js mobile-first frontend
- `ai-service/`: Python FastAPI AI service
- `infra/`: Docker Compose and infrastructure
- `docs/`: Obsidian vault and thesis documentation
- `.agents/skills/`: repository-local Codex skills

## Core constraints

- MVP first.
- Scope is Ho Chi Minh City with roughly 30–100 places.
- Do not add AR, Unity, native mobile, microservices, or complex deep learning to MVP.
- Backend is Spring Boot modular monolith, package-by-feature.
- PostgreSQL is the source of truth.
- pgvector is introduced when RAG is implemented.
- Flyway owns schema changes.
- Hibernate must use `ddl-auto: validate`.
- Do not return JPA entities from REST controllers.
- Prefer constructor injection.
- Never commit `.env`, credentials, API keys, or secrets.
- Do not present unverified place data as factual.

## Workflow

For significant or ambiguous work:

1. Invoke `$grill-with-docs`.
2. Create or update a feature spec in `docs/01-Requirements/Features/`.
3. Ask for explicit approval.
4. Produce a file-by-file implementation plan.
5. Implement in small reviewable steps.
6. Run relevant tests.
7. Review the diff.
8. Update technical docs, thesis notes, and project context.

Do not use GitHub Spec Kit unless the user explicitly reverses the current decision.

For small and obvious fixes, the full workflow may be skipped, but all constraints still apply.

## Prompt quality

Implementation tasks should state:

- Goal
- Context files
- Constraints
- Done when
- Verification command

## Verification

Do not claim completion without listing commands run and their results.

Backend baseline:

```bash
cd backend
./mvnw test
```

Before release or Docker packaging:

```bash
cd backend
./mvnw clean verify
```

## Documentation

Meaningful features must update:

- feature spec;
- relevant architecture/database/API note;
- development log;
- thesis section mapping;
- `PROJECT_CONTEXT.md` if project status changes.

## Learning documentation

The owner is learning the technologies used in this project. When the owner
says that a concept is unfamiliar, says they have not used or learned it before,
or asks for an educational explanation of project code:

1. Explain the concept in the conversation using the current source code.
2. Create or update a topic-focused Markdown note under
   `docs/05-Testing-and-Learning/` in the same turn.
3. Include the motivation, execution flow, important code, alternatives,
   trade-offs, and when the approach is or is not appropriate.
4. Link the note to relevant feature/source files and use examples from this
   repository where possible.
5. Update an existing topic note instead of creating duplicate notes.

Documentation-only learning updates do not require the significant-feature
approval workflow unless they also change product requirements or production
code.
