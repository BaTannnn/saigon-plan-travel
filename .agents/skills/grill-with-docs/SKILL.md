---
name: grill-with-docs
description: Interview the user one question at a time, inspect the repository, and update durable project documentation before implementation. Use for new features, database/API changes, architecture decisions, RAG, scheduling, time-series/context, replanning, or any ambiguous task. Do not use for tiny well-scoped fixes.
---

# Grill With Docs

Establish shared understanding before code is written.

## Grounding

Before asking questions:

1. Read root `AGENTS.md`.
2. Read the nearest relevant `AGENTS.md`.
3. Read `docs/00-Dashboard/PROJECT_CONTEXT.md`.
4. Inspect relevant code.
5. Read related feature specs, ADRs, database notes, and API notes.

Do not ask the user for information that can be determined safely from the repository.

## Interview behavior

- Ask exactly one question at a time.
- Ask the highest-impact unresolved question first.
- Challenge assumptions politely and directly.
- Do not accept vague words such as "smart", "real-time", "fast", "optimized", or "AI-powered" without measurable meaning.
- Prefer bounded options when useful, while allowing a custom answer.
- Do not write production code during the interview.
- Do not silently decide scope, architecture, data ownership, external API behavior, or evaluation criteria.

Investigate only the dimensions relevant to the task:

- target user and problem;
- in-scope and out-of-scope behavior;
- inputs and outputs;
- business rules;
- data ownership and data freshness;
- database/API implications;
- edge cases and failure modes;
- external service limits;
- acceptance criteria;
- tests and proof commands;
- thesis/report implications.

## Documentation locations

Do not create a second root context file.

Use:

- Project status and durable context:
  `docs/00-Dashboard/PROJECT_CONTEXT.md`
- Feature requirements:
  `docs/01-Requirements/Features/`
- Domain vocabulary:
  `docs/01-Requirements/Domain-Glossary.md`
- Architecture decisions:
  `docs/02-Architecture/ADR/`
- Database notes:
  `docs/03-Database/`
- API notes:
  `docs/04-API/`
- Development log:
  `docs/09-Development-Log/`

## Decision rules

- Update the glossary only after terminology is confirmed.
- Create an ADR only for a decision that is costly or difficult to reverse.
- Create or update one feature spec for the feature being discussed.
- Keep feature specs concise and testable.
- Do not modify production code until the user approves the locked requirements.

## Stop condition

Stop asking questions when:

- goal and user value are clear;
- scope boundaries are explicit;
- inputs, outputs, business rules, and failure behavior are defined;
- acceptance criteria are testable;
- major architectural decisions are confirmed or intentionally deferred.

Then produce:

1. `LOCKED REQUIREMENTS`
2. `OUT OF SCOPE`
3. `BUSINESS RULES`
4. `EDGE CASES`
5. `ACCEPTANCE CRITERIA`
6. `OPEN RISKS`
7. `DOCUMENTATION UPDATES`
8. `RECOMMENDED NEXT STEP`

Ask for explicit approval.

After approval:

1. Create/update the feature spec from `FEATURE_TEMPLATE.md`.
2. Produce a file-by-file implementation plan.
3. Wait for approval before implementation.
