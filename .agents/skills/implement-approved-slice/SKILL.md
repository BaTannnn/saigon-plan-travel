---
name: implement-approved-slice
description: Implement one bounded SaigonPlanTravel coding slice whose requirements are already known. Use for “continue”, “implement this step”, focused feature work, or a minimum bug fix. Preserve dirty work, avoid unrelated refactors/overengineering, run relevant checks, explain the actual flow, then stop. Do not use for new ambiguous product/architecture decisions.
---

# Implement Approved Slice

Implement quickly without turning Codex into a black box.

## 1. Grounding before edits

1. Run `git status --short` and `git branch --show-current`.
2. Read root and nearest `AGENTS.md`.
3. Read `PROJECT_CONTEXT.md` and `CURRENT_IMPLEMENTATION.md`.
4. Read the exact feature-spec sections for this slice.
5. Inspect the source, migration/API contract, tests, and current diff involved.

If a target file is dirty, inspect its diff and preserve the owner's work.

## 2. Confirm scope internally

Before editing, state a short plan containing:

- goal;
- files expected to change;
- files explicitly out of scope;
- contract/invariant being preserved;
- verification command.

Do not start a requirements interview if these are already clear.

## 3. Implementation rules

- Implement one coherent behavior, not the entire remaining feature.
- No unrelated cleanup/refactor.
- No new production dependency without explicit approval.
- No migration rewrite of an already-applied/shared version.
- No cross-module repository/entity shortcut around an existing read contract.
- Prefer the simplest readable design that satisfies the current requirement.
- Do not create speculative manager/factory/policy/result hierarchies.
- Add only the smallest useful tests at the correct layer.
- Prefer real immutable value objects/records in tests; mock external/repository boundaries.
- Keep controllers thin and domain logic free of unnecessary framework/network concerns.

If the task unexpectedly requires a new public contract, migration, dependency,
architecture decision, or algorithm rule, stop before expanding scope and report it.

## 4. Verification

Run the narrowest relevant test/check first. Then run the broader project check when
feasible and appropriate.

Never claim a check passed if it could not run. Include the actual failure reason.

## 5. Self-review before response

Inspect the diff and ask:

- Did I touch anything outside the slice?
- Did I overwrite pre-existing dirty work?
- Is there a simpler implementation?
- Did I duplicate a rule already owned elsewhere?
- Did I add test complexity without useful proof?
- Did I violate module/API/database/security boundaries?

Simplify or fix issues within scope before reporting completion.

## 6. Required completion response

Return:

1. `RESULT`
2. `CHANGED FILES`
3. `EXECUTION FLOW`
4. `WHY THIS DESIGN`
5. `WHAT THE OWNER SHOULD UNDERSTAND`
6. `VERIFICATION`
7. `DIFF AUDIT`
8. `MANUAL CHECK`
9. `NEXT SLICE`

Recommend exactly one next slice and stop. Do not implement it automatically.
