CREATE EXTENSION IF NOT EXISTS unaccent;

CREATE INDEX IF NOT EXISTS idx_place_categories_category_place
    ON place_categories (category_id, place_id);
