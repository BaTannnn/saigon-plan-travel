-- SaigonPlanTravel V2 foundation extensions.
-- vector is enabled now so the database is ready for the later RAG phase,
-- but no vector table is created in the foundation schema.

CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS unaccent;
CREATE EXTENSION IF NOT EXISTS pgcrypto;
