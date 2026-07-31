CREATE TABLE trip_category_preferences
(
    trip_id     BIGINT NOT NULL,
    category_id BIGINT NOT NULL,

    CONSTRAINT pk_trip_category_preferences
        PRIMARY KEY (trip_id, category_id),

    CONSTRAINT fk_trip_category_preferences_trip
        FOREIGN KEY (trip_id)
            REFERENCES trips (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_trip_category_preferences_category
        FOREIGN KEY (category_id)
            REFERENCES categories (id)
            ON DELETE RESTRICT
);

CREATE INDEX idx_trip_category_preferences_category_trip
    ON trip_category_preferences (category_id, trip_id);