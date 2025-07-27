CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS vector_store (
    id UUID PRIMARY KEY,
    content TEXT,
    metadata JSONB,
    embedding VECTOR(768)  -- 768 is the correct dimension for nomic-embed-text
);

INSERT INTO roles (name, description, created_at, updated_at)
VALUES
  ('ADMIN', 'Administrator role – full access', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('USER', 'Standard user role – limited access', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
