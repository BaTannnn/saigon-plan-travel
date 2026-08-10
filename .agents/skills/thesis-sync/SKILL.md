---
name: thesis-sync
description: Convert a verified SaigonPlanTravel implementation milestone into thesis/Obsidian documentation grounded in actual source, migrations, tests, and measured evidence. Use after a feature or coherent milestone works. Update docs only; do not change production code or invent results.
---

# Thesis Sync

Document what was actually built and proven.

## Grounding

Read:

1. relevant FEAT spec;
2. actual implementation source;
3. migrations/schema;
4. tests and real test output/evidence available in the repo/conversation;
5. current architecture/API/database notes;
6. project report structure/template when available.

Do not treat planned features as implemented results.

## Map content to thesis sections

Use these mappings unless the report template dictates different numbering:

- theory/formulas/algorithms → `Cơ sở lý thuyết`;
- module boundaries, database, API, sequence/data flow → `Phân tích và thiết kế hệ thống`;
- implementation classes/config/technology → `Cài đặt và triển khai`;
- test cases, measurements, comparisons → `Thực nghiệm và đánh giá`;
- deferred capabilities/known limitations → `Hướng phát triển`.

## Evidence rules

- State whether data is demo/simulated/verified.
- Haversine must be described as straight-line distance, not real route distance.
- Forecast/context estimates must not be called real-time observation unless the actual
  source/contract supports that wording.
- RAG claims must distinguish retrieval evidence from LLM-generated wording.
- Do not report latency/accuracy/pass counts unless measured evidence exists.

## Documentation behavior

Prefer updating existing notes over creating duplicates.
Useful locations:

- `docs/02-Architecture/`
- `docs/03-Database/`
- `docs/04-API/`
- `docs/05-Testing-and-Learning/`
- `docs/09-Development-Log/`
- thesis-specific notes if present.

Update `PROJECT_CONTEXT.md` / `CURRENT_IMPLEMENTATION.md` only when milestone status
actually changes.

## Required output

1. `IMPLEMENTED EVIDENCE`
2. `THESIS SECTION MAPPING`
3. `DOCS UPDATED`
4. `DRAFT ACADEMIC TEXT`
5. `FIGURES/TABLES WORTH ADDING`
6. `LIMITATIONS TO STATE`
7. `MISSING EVIDENCE BEFORE CLAIMING COMPLETION`
