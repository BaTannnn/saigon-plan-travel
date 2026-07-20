CREATE TABLE place_categories (
    place_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,

    PRIMARY KEY (place_id, category_id),

    CONSTRAINT fk_place_categories_place
        FOREIGN KEY (place_id)
        REFERENCES places(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_place_categories_category
        FOREIGN KEY (category_id)
        REFERENCES categories(id)
        ON DELETE CASCADE
);