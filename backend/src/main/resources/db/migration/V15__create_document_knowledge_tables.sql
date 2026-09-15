CREATE TABLE knowledge_documents (
    id BIGSERIAL PRIMARY KEY,

    title VARCHAR(255) NOT NULL,
    source_label VARCHAR(255) NOT NULL,
    source_uri TEXT NOT NULL,
    content_hash CHAR(64) NOT NULL,

    created_at TIMESTAMP WITH TIME ZONE
        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP WITH TIME ZONE
        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_knowledge_documents_title
        CHECK (BTRIM(title) <> ''),

    CONSTRAINT chk_knowledge_documents_source_label
        CHECK (BTRIM(source_label) <> ''),

    CONSTRAINT chk_knowledge_documents_source_uri
        CHECK (BTRIM(source_uri) <> ''),

    CONSTRAINT uq_knowledge_documents_source_uri
        UNIQUE (source_uri)
);

CREATE TABLE document_knowledge_chunks (
    id BIGSERIAL PRIMARY KEY,

    document_id BIGINT NOT NULL,
    chunk_index INTEGER NOT NULL,
    page_number INTEGER NOT NULL,
    content TEXT NOT NULL,
    content_hash CHAR(64) NOT NULL,
    embedding VECTOR(768) NOT NULL,

    created_at TIMESTAMP WITH TIME ZONE
        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_document_knowledge_chunks_document
        FOREIGN KEY (document_id)
        REFERENCES knowledge_documents(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_document_knowledge_chunks_index
        CHECK (chunk_index > 0),

    CONSTRAINT chk_document_knowledge_chunks_page
        CHECK (page_number > 0),

    CONSTRAINT chk_document_knowledge_chunks_content
        CHECK (BTRIM(content) <> ''),

    CONSTRAINT uq_document_knowledge_chunks_index
        UNIQUE (document_id, chunk_index)
);
