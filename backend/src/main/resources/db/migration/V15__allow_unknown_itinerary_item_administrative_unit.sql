ALTER TABLE itinerary_items
    ALTER COLUMN place_administrative_unit_name DROP NOT NULL,
    ALTER COLUMN place_administrative_unit_type DROP NOT NULL;

ALTER TABLE itinerary_items
    ADD CONSTRAINT chk_itinerary_items_administrative_unit_pair
        CHECK (
            (
                place_administrative_unit_name IS NULL
                AND place_administrative_unit_type IS NULL
            )
            OR
            (
                place_administrative_unit_name IS NOT NULL
                AND place_administrative_unit_type IS NOT NULL
            )
        );
