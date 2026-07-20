CREATE TABLE places (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    slug VARCHAR(180) NOT NULL UNIQUE,
    short_description VARCHAR(500),
    full_description TEXT,
    address VARCHAR(255) NOT NULL,
    district VARCHAR(100) NOT NULL,

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
        CHECK (max_cost >= min_cost)
);