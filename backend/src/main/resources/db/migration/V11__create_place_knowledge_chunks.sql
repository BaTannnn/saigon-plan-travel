CREATE TABLE place_knowledge_chunks (
    id BIGSERIAL PRIMARY KEY,

    place_id BIGINT NOT NULL,

    chunk_index INTEGER NOT NULL,

    section VARCHAR(30) NOT NULL,

    content TEXT NOT NULL,

    source_label VARCHAR(255) NOT NULL,
    source_uri TEXT NOT NULL,
    retrieved_at DATE NOT NULL,

    language VARCHAR(10) NOT NULL,

    content_hash CHAR(64) NOT NULL,

    embedding VECTOR(768) NOT NULL,

    created_at TIMESTAMP WITH TIME ZONE
        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP WITH TIME ZONE
        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_place_knowledge_chunks_place
        FOREIGN KEY (place_id)
        REFERENCES places(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_place_knowledge_chunks_index
        CHECK (chunk_index > 0),

    CONSTRAINT chk_place_knowledge_chunks_section
        CHECK (
            section IN (
                'OVERVIEW',
                'BACKGROUND',
                'HIGHLIGHTS',
                'EXPERIENCE'
            )
        ),

    CONSTRAINT chk_place_knowledge_chunks_content
        CHECK (BTRIM(content) <> ''),

    CONSTRAINT chk_place_knowledge_chunks_language
        CHECK (language = 'vi-VN'),

    CONSTRAINT uq_place_knowledge_chunks_index
        UNIQUE (place_id, chunk_index),

    CONSTRAINT uq_place_knowledge_chunks_content
        UNIQUE (place_id, content_hash)
);