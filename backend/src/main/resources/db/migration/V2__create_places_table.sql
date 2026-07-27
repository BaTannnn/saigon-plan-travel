CREATE TABLE places (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    slug VARCHAR(180) NOT NULL UNIQUE,
    short_description VARCHAR(500),
    full_description TEXT,
    address VARCHAR(255) NOT NULL,
    administrative_unit_name VARCHAR(100),
    administrative_unit_type VARCHAR(20),

    latitude NUMERIC(10, 7) NOT NULL,
    longitude NUMERIC(10, 7) NOT NULL,

    estimated_visit_minutes INTEGER NOT NULL,
    min_cost NUMERIC(12, 2) NOT NULL DEFAULT 0,
    max_cost NUMERIC(12, 2) NOT NULL DEFAULT 0,

    indoor BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_places_latitude
        CHECK (latitude BETWEEN -90 AND 90),

    CONSTRAINT chk_places_longitude
        CHECK (longitude BETWEEN -180 AND 180),

    CONSTRAINT chk_places_visit_minutes
        CHECK (estimated_visit_minutes > 0),

    CONSTRAINT chk_places_min_cost
        CHECK (min_cost >= 0),

    CONSTRAINT chk_places_max_cost
        CHECK (max_cost >= min_cost),

    CONSTRAINT chk_places_administrative_unit_pair
        CHECK (
            (administrative_unit_name IS NULL AND administrative_unit_type IS NULL)
            OR
            (administrative_unit_name IS NOT NULL AND administrative_unit_type IS NOT NULL)
        ),

    CONSTRAINT chk_places_administrative_unit_name_not_blank
        CHECK (
            administrative_unit_name IS NULL
            OR LENGTH(TRIM(administrative_unit_name)) > 0
        ),

    CONSTRAINT chk_places_administrative_unit_type
        CHECK (
            administrative_unit_type IS NULL
            OR administrative_unit_type IN ('WARD', 'COMMUNE', 'SPECIAL_ZONE')
        )
);
