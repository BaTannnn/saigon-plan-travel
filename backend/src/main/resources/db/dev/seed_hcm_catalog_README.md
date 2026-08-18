# SaigonPlanTravel – HCMC development seed

File: `seed_hcm_catalog.sql`

## Purpose

This dataset is intended for **local development, frontend demo, manual itinerary, scheduling and thesis experiments**.

It is deliberately kept **outside `db/migration`** because the current backend integration tests rely on the canonical `V11__seed_demo_data.sql` dataset and its exact counts.

## Dataset coverage

- 42 HCMC attractions / points of interest
- 13 categories
- 126 place-category mappings
- 294 opening-hour rows (7 rows for every seeded place)
- Real-world place names and approximate coordinates
- Estimated visit duration, min/max cost and indoor/outdoor flag
- All 5 current AI corpus slugs are present:
  - `dinh-doc-lap`
  - `bao-tang-my-thuat-tphcm`
  - `buu-dien-trung-tam-sai-gon`
  - `thao-cam-vien-sai-gon`
  - `cho-ben-thanh`

## Data-quality note

Names and locations represent real HCMC attractions, but **opening hours and costs are development estimates** and must not be treated as real-time official data.

This is intentional for the MVP: it gives the scheduler complete structured data while allowing the thesis to clearly distinguish static curated data from future real-time/context data.

## How to use

Recommended project location:

```text
backend/src/main/resources/db/dev/seed_hcm_catalog.sql
```

Run from project root after PostgreSQL/Flyway has initialized the schema:

```bash
docker compose --env-file .env -f infra/compose.yaml exec -T postgres   sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"'   < backend/src/main/resources/db/dev/seed_hcm_catalog.sql
```

The script is idempotent for its own seeded catalog. Re-running it updates place/category fields and rebuilds category/opening-hour mappings.

## Important RAG limitation

The database catalog will contain 42 places, but the current `ai-service/corpus` contains knowledge documents for only 5 places.

Therefore:

- Manual place browsing / add / replace can use all 42 places.
- Automatic RAG itinerary generation can currently retrieve only those 5 corpus-backed places.

For a stronger automatic-generation demo, expand the AI corpus to at least 15–20 of these seeded places next.
