CREATE TABLE opening_hours (
    id BIGSERIAL PRIMARY KEY,
    place_id BIGINT NOT NULL,
    day_of_week SMALLINT NOT NULL,
    open_time TIME,
    close_time TIME,
    closed BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_opening_hours_place
        FOREIGN KEY (place_id)
        REFERENCES places(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_opening_hours_place_day
        UNIQUE (place_id, day_of_week),

    CONSTRAINT chk_opening_hours_day
        CHECK (day_of_week BETWEEN 1 AND 7),

    CONSTRAINT chk_opening_hours_time
        CHECK (
        closed = TRUE
           OR (
           open_time IS NOT NULL
               AND close_time IS NOT NULL
               AND open_time < close_time
           )
        )
);