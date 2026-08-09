# VIBE_ENGINEERING_WORKFLOW — Codex without losing project understanding

## 1. Goal

Use Codex as an engineering accelerator, not as a black-box code generator.
The desired loop is:

```text
source-grounded context
→ bounded plan
→ Codex implements one coherent slice
→ tests
→ diff review
→ teach-back
→ owner understands/accepts
→ commit
```

The owner should be able to explain every important boundary and execution flow even if
Codex typed most of the boilerplate.

## 2. Five repo skills

### `$project-context-audit`

Use at the start of a new module, after a long break, or when the chat has lost context.
It reads source/docs/git state and produces a read-only current-state map.

### `$implement-approved-slice`

Default coding skill when requirements are already known. It preserves dirty work,
implements one coherent slice, runs checks, audits the diff, and stops.

### `$review-and-teach`

Use after code generation or before committing. It reviews correctness/scope and teaches
the actual runtime/data flow without changing code by default.

### `$grill-with-docs`

Use only for a genuinely new or ambiguous feature/rule. It asks one high-impact question
at a time and locks requirements before code.

### `$thesis-sync`

Use after a verified milestone to convert actual implementation/evidence into thesis and
Obsidian material.

## 3. Daily session protocol

### Step A — Start cleanly

From the repository root:

```bash
git status --short
git branch --show-current
```

Do not hide a dirty worktree from Codex. Existing changes are context.

### Step B — Keep one chat per coherent outcome

Good threads:

- “Finish FEAT-005 E1 Itinerary parent mapping”
- “Implement FEAT-005 E2 ItineraryItem mapping”
- “Debug one failing integration test”
- “Connect Trip form to existing API”

Bad thread:

- “Finish the whole project”

A coherent thread keeps context useful and makes the diff reviewable.

### Step C — Ground before coding

For unfamiliar work:

```text
Use $project-context-audit.
Audit the current source for FEAT-005 persistence.
Do not modify files. Tell me the actual current state, dirty files, relevant
migrations/contracts, risks, and the smallest next slice.
```

For a continuation whose scope is already known, go directly to the implementation skill.

### Step D — Let Codex implement a bounded slice

Example for the current repository state:

```text
Use $implement-approved-slice.
Continue FEAT-005 E1 from the current dirty Itinerary.java.

Goal:
Finish only the Itinerary parent + preferred-category JPA mapping against V13.

Read first:
- AGENTS.md
- backend/AGENTS.md
- docs/00-Dashboard/PROJECT_CONTEXT.md
- docs/00-Dashboard/CURRENT_IMPLEMENTATION.md
- FEAT-005 sections 12, 13, 17.E1
- V13 migration
- current Itinerary.java diff

Constraints:
- Preserve my existing uncommitted Itinerary.java work.
- Do not implement ItineraryItem, warnings, repository, service, or controller.
- Do not change V1–V14.
- Do not add dependencies.
- Keep JPA mapping beginner-readable.

Done when:
- mapping matches V13;
- production compiles;
- relevant tests/checks are run if available;
- you explain every field group/annotation and stop.
```

This prompt gives Codex freedom to type and verify code while preventing it from expanding
the feature.

### Step E — Review before accepting

Run:

```text
Use $review-and-teach.
Review the current uncommitted diff against FEAT-005 E1 and V13.
Do not edit code. Flag bugs, overengineering, boundary violations, missing tests,
and then explain the execution/persistence flow so I can defend it.
```

Use Codex's diff/review UI or `/review` when available, then inspect the changed files in
your editor.

### Step F — Owner teach-back

Before committing an important slice, answer these five questions yourself:

1. What problem does this code solve?
2. Which class owns the behavior and why?
3. What comes in and what goes out?
4. Which database/API/module boundary does it depend on?
5. What test proves the important rule?

If one answer is unclear, ask `$review-and-teach` about that exact point before moving on.

### Step G — Commit a coherent unit

Prefer one feature branch and several understandable commits, instead of a branch for every
class.

Example:

```text
feat/itineraries
  - feat: implement deterministic greedy scheduling core
  - feat: map itinerary aggregate persistence
  - feat: expose itinerary generation api
```

Do not commit secrets, generated build output, node_modules, IDE state, or temporary AI
scratch files.

## 4. Prompt templates

### Implement a known backend slice

```text
Use $implement-approved-slice.

Task: <one coherent behavior>
Feature: <FEAT-xxx / micro-step>

Read first:
- <spec sections>
- <migration/API/source files>

Must preserve:
- <existing contract/boundary>

Do not:
- <out-of-scope work>

Verification:
- <targeted command>
- <checkpoint command if appropriate>

After coding, explain the runtime flow and stop.
```

### Debug a failure

```text
Audit this failure from the first/root cause, not the repeated downstream errors.
Inspect current source and git diff before editing.
Reproduce with the smallest command possible.
Explain the cause, then make the minimum fix consistent with the existing contract.
Do not weaken production behavior just to make a test pass.
```

### Review for overengineering

```text
Use $review-and-teach.
Review the current diff only.
Specifically look for unnecessary classes, duplicated policies, premature abstractions,
extra tests, cross-module leakage, and behavior not required by the feature.
Recommend deletions/simplifications before additions.
Do not edit until I approve.
```

### Learn a generated change

```text
Use $review-and-teach on <files/commit>.
Explain from request entry point to database/domain result.
For each non-obvious class, tell me:
- why it exists;
- why it is in this package;
- what would break if removed;
- one simpler alternative and why we did/did not choose it.
Then quiz me with 5 short questions.
```

### Sync thesis after implementation

```text
Use $thesis-sync for FEAT-005.
Use only actual source, migrations, tests, and measured evidence.
Update the appropriate thesis/Obsidian notes with:
- problem and objective;
- design decision;
- algorithm/data flow;
- implementation;
- evaluation evidence;
- limitations and future work.
Do not change production code.
```

## 5. When Codex may work more autonomously

Codex may complete a whole slice without asking between files when all are true:

- requirements are already settled;
- the files form one behavior;
- no new dependency/architecture/database contract is introduced;
- the working tree is understood;
- verification is clear;
- the diff remains reviewable.

Examples:

- entity + small mapper required for one persistence slice;
- request/response DTO pair for one endpoint;
- a bug fix plus its regression test.

It should stop and request a scope decision when a task unexpectedly requires a new
migration, public API change, cross-module dependency, provider, or algorithm rule.

## 6. Anti-patterns for this project

Avoid prompts like:

```text
Implement FEAT-005 completely.
Refactor the backend to best practices.
Make the system production-ready.
Improve the architecture.
Add AI wherever useful.
```

They invite scope growth and make learning difficult.

Prefer concrete outcome prompts tied to source and a verification command.

## 7. Thesis-friendly engineering evidence

At each milestone preserve evidence that can later enter the report:

- feature requirement/decision;
- architecture/data-flow diagram;
- migration/schema;
- algorithm formula/pseudocode;
- focused test proving the rule;
- integration/smoke result;
- measured limitation or assumption;
- before/after comparison for routing/context/replanning when available.

This is more useful for defense than maximizing class count or model complexity.
