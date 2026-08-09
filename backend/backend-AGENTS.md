# Backend Engineering Guidelines

These instructions apply to all work under `backend/` and extend the repository root `AGENTS.md`.

## Backend stack

The backend is a Spring Boot modular monolith using:

- Java
- Spring MVC
- Spring Security
- Spring Data JPA
- Bean Validation
- PostgreSQL
- Flyway

Always inspect the actual `pom.xml`, source code, configuration and migrations before relying on version-specific framework behavior.

## Architecture

Organize code by feature/module rather than by a single global technical-layer structure.

A feature may contain packages such as:

```text
feature/
├── controller/
├── dto/
├── entity/
├── mapper/
├── repository/
├── service/
└── domain/
```

Not every feature needs every package.

Keep dependencies between feature modules explicit. A module must not access another module's repository or JPA entity merely for convenience. Prefer an intentional service, query interface, snapshot or read model when crossing module boundaries.

Avoid circular dependencies.

## Layer responsibilities

Controllers handle HTTP concerns only:

```text
request
→ binding / validation / authentication
→ application service
→ HTTP response
```

Do not place database queries, persistence logic or core business rules in controllers.

Application/service code coordinates use cases, module contracts and transaction boundaries.

Domain code contains business rules that can remain independent from Spring when practical.

Repository code owns persistence queries and database access.

Do not create additional abstraction layers unless they solve a real problem in the active feature.

## Persistence

Flyway is the source of truth for the database schema. Hibernate validates the schema rather than creating or mutating it.

Never modify an already-applied migration. Before creating a new migration, inspect the existing migration files and actual migration history and choose the next valid version.

JPA mappings must match the migration schema exactly, including:

- column names;
- nullability;
- precision and scale;
- enum representation;
- foreign keys;
- indexes when relevant to repository behavior;
- collection and join tables.

Use `BigDecimal` for money and persisted decimal measurements.

Do not serialize JPA entities or lazy persistence collections directly through REST APIs.

Prefer scalar foreign keys or immutable snapshot values instead of cross-module JPA relationships when that preserves the intended module boundary.

## Entity and DTO conventions

Entities should normally have:

- a protected no-argument constructor;
- no Lombok `@Data`;
- no class-wide public setters;
- meaningful mutation methods or factories when behavior exists.

Prefer Java records for immutable DTOs, snapshots and read models when suitable.

Do not expose mutable internal collections directly. Return immutable copies or purpose-specific views when callers only need to read them.

## API conventions

Public APIs live under `/api/v1` unless an existing contract explicitly says otherwise.

Keep existing API contracts backward compatible unless the active feature explicitly approves a contract change.

Reuse the project's existing `ProblemDetail` error format rather than creating a competing error envelope.

Do not expose internal database identifiers when a public identifier contract already exists.

Authentication and ownership must be derived from the authenticated principal, not from client-supplied ownership identifiers.

Keep controllers thin and keep transport concerns out of domain logic.

## Transactions and external calls

Keep transaction boundaries in the application/service layer.

Use read-only transactions for database-only read use cases when appropriate.

External HTTP, AI, routing, weather or other provider calls should not remain inside long-running database transactions.

When a use case persists one aggregate or one logical result, persistence should be atomic when required by the active feature contract.

## Cross-module boundaries

When one feature needs data owned by another feature:

1. inspect the existing public/internal contract first;
2. reuse that contract when it fits the use case;
3. otherwise introduce the smallest purpose-specific read/service boundary necessary;
4. do not bypass the boundary by importing another feature's repository or entity.

Do not move feature-specific records into another module's core domain just to shorten imports.

## Testing

Test behavior at the layer where it belongs.

Use:

- ordinary JUnit tests for pure domain logic;
- mocks for orchestration dependencies and external boundaries when useful;
- PostgreSQL/Testcontainers for database behavior, migrations and repository semantics;
- MockMvc/web tests for HTTP and security contracts.

Prefer real immutable value objects over mocking them.

Avoid duplicating the same invariant across many test layers.

Do not change production design solely to make a test easier.

Run focused tests while developing. Before a backend feature checkpoint, run the full backend test suite when feasible.

## Engineering guardrails

Before adding a new class, interface, abstraction, dependency or framework, check whether the active behavior can be expressed clearly with the existing design.

Avoid infrastructure or generic abstractions added only for hypothetical future requirements.

Prefer the simplest implementation that satisfies the current feature contract and remains readable to the project owner.

Do not refactor unrelated modules while implementing a focused feature unless the active task explicitly requires it.

Do not silently change public API contracts, migration history, security semantics or module boundaries.

If the feature specification, documentation and current source disagree, inspect the source of truth and report the conflict before making a speculative change.

## Source-of-truth order

For backend work, use this precedence when context conflicts:

1. current source code and applied migrations;
2. automated tests that describe current behavior;
3. `docs/00-Dashboard/CURRENT_IMPLEMENTATION.md`;
4. `docs/02-Architecture/CODEBASE_MAP.md`;
5. the active feature specification;
6. older planning or archived documentation.

Do not force working source to match stale documentation without an explicit decision.

## Required context for implementation tasks

Before modifying backend code, inspect the smallest relevant set of sources:

- repository root `AGENTS.md`;
- this `backend/AGENTS.md`;
- current `git status` and relevant uncommitted diff;
- current source files involved in the task;
- relevant migrations;
- relevant tests;
- the active feature specification;
- `CURRENT_IMPLEMENTATION.md` or `CODEBASE_MAP.md` when module/project context is needed.

Do not read or rewrite the whole repository when a focused slice can be understood from a smaller context.

## Before reporting completion

Check that the change:

1. stays within the approved scope;
2. preserves module boundaries;
3. matches Flyway/JPA/API contracts;
4. does not introduce unnecessary abstractions;
5. has appropriate verification;
6. does not overwrite unrelated uncommitted work;
7. can be explained clearly in terms of business flow and architecture.

For current feature progress, concrete package locations and implementation status, read:

- `docs/00-Dashboard/CURRENT_IMPLEMENTATION.md`
- `docs/02-Architecture/CODEBASE_MAP.md`
- the active `FEAT-xxx` specification.
