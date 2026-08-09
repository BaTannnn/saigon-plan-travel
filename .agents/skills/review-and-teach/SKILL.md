---
name: review-and-teach
description: Review a SaigonPlanTravel diff/commit/source area for correctness, scope, overengineering, architecture boundaries, tests, and regressions, then explain the runtime/data flow so the owner can defend it. Use after Codex-generated code, before commit, or when the owner asks why/how code works. Read-only unless fixes are explicitly requested.
---

# Review And Teach

Default to review-only. Do not edit files unless the owner explicitly asks for fixes.

## Grounding

1. Read root/nearest `AGENTS.md`.
2. Inspect `git status` and the exact diff/commit/files requested.
3. Read relevant feature requirements, migrations, API contracts, and neighboring source.
4. Distinguish pre-existing changes from the diff under review.

## Review order

Review in this order:

1. Correctness against current behavior/requirements.
2. Data/schema/API compatibility.
3. Authentication/ownership/security boundary.
4. Module dependency direction.
5. Determinism/edge cases for scheduling logic.
6. Transaction/lazy-loading/query implications for JPA code.
7. Test value and missing regression proof.
8. Overengineering and simplification opportunities.
9. Naming/readability only after behavior is sound.

Do not recommend abstractions just because they are common patterns.

## Teach-back explanation

Explain the code using the actual repository flow:

```text
entry point
→ service/orchestrator
→ domain/read contract
→ repository/database or result
→ response
```

For each non-obvious class/type, answer:

- What responsibility does it own?
- Why is it in this package?
- Who calls it?
- What invariant does it protect?
- What simpler alternative exists?
- Why is the current level of complexity justified or not justified?

When an algorithm/formula is involved, explain inputs, formula/rules, output, and one
small numeric/example scenario.

## Required output

1. `REVIEW VERDICT`
2. `FINDINGS` ordered high → low severity
3. `OVERENGINEERING CHECK`
4. `ACTUAL EXECUTION FLOW`
5. `KEY CONCEPTS TO LEARN`
6. `TEST / VERIFICATION GAPS`
7. `5 OWNER CHECK QUESTIONS`
8. `RECOMMENDED ACTION`

If there are no meaningful findings, say so directly rather than inventing issues.
