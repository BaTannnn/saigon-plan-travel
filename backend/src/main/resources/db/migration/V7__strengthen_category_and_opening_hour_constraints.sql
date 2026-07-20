-- FEAT-002 forward constraints. V3-V5 are immutable and must not be edited.

ALTER TABLE categories
    ADD CONSTRAINT chk_categories_slug_format
        CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$');

ALTER TABLE opening_hours
    ADD CONSTRAINT chk_opening_hours_state
        CHECK (
            (
                closed = TRUE
                AND open_time IS NULL
                AND close_time IS NULL
            )
            OR
            (
                closed = FALSE
                AND open_time IS NOT NULL
                AND close_time IS NOT NULL
                AND open_time < close_time
            )
        );
