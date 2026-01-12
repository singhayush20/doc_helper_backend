-- create vector store
CREATE EXTENSION IF NOT EXISTS vector;
CREATE TABLE IF NOT EXISTS vector_store (
  id UUID PRIMARY KEY,
  content TEXT,
  metadata JSONB,
  embedding VECTOR(1536)
);
-- Insert roles in the db
INSERT INTO roles (name, description, created_at, updated_at)
VALUES (
    'ADMIN',
    'Administrator role – full access',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
  ),
  (
    'USER',
    'Standard user role – limited access',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
  );
-- create schema for persisting token usage data
-- Token Usage Table
CREATE TABLE user_token_usage (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL,
  document_id BIGINT,
  thread_id VARCHAR(255),
  message_id VARCHAR(255),
  timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  prompt_tokens BIGINT NOT NULL,
  completion_tokens BIGINT NOT NULL,
  total_tokens BIGINT NOT NULL,
  model_name VARCHAR(100),
  operation_type VARCHAR(50),
  estimated_cost NUMERIC(10, 6),
  duration_ms BIGINT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
-- Indexes for optimal query performance
CREATE INDEX idx_user_timestamp ON user_token_usage(user_id, timestamp DESC);
CREATE INDEX idx_document_user ON user_token_usage(document_id, user_id);
CREATE INDEX idx_timestamp ON user_token_usage(timestamp DESC);
CREATE INDEX idx_thread_id ON user_token_usage(thread_id);
-- User Token Quota Table
CREATE TABLE user_token_quota (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL UNIQUE,
  monthly_limit BIGINT NOT NULL DEFAULT 100000,
  current_monthly_usage BIGINT NOT NULL DEFAULT 0,
  reset_date TIMESTAMPTZ NOT NULL,
  tier VARCHAR(50) DEFAULT 'free',
  is_active BOOLEAN DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_quota_user_id ON user_token_quota(user_id);
CREATE INDEX idx_quota_reset_date ON user_token_quota(reset_date);
-- Materialized view for monthly summaries (refresh periodically)
CREATE MATERIALIZED VIEW monthly_usage_summary AS
SELECT user_id,
  DATE_TRUNC('month', timestamp) AS month,
  SUM(total_tokens) AS total_tokens,
  SUM(estimated_cost) AS total_cost,
  COUNT(*) AS request_count,
  AVG(duration_ms) AS avg_duration_ms,
  MAX(timestamp) AS last_request_time
FROM user_token_usage
GROUP BY user_id,
  DATE_TRUNC('month', timestamp);
CREATE UNIQUE INDEX idx_monthly_summary_user_month ON monthly_usage_summary(user_id, month);
-- Function to refresh monthly summary
CREATE OR REPLACE FUNCTION refresh_monthly_summary() RETURNS void AS $$ BEGIN REFRESH MATERIALIZED VIEW CONCURRENTLY monthly_usage_summary;
END;
$$ LANGUAGE plpgsql;

-- Enable pg_trgm extension for substring/fuzzy search
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Create GIN index on original_file_name for fast substring and fuzzy match
CREATE INDEX IF NOT EXISTS idx_documents_originalfilename_trgm ON documents USING gin (
    original_file_name gin_trgm_ops
);

CREATE INDEX idx_subscription_status_expiry ON subscription (status, checkout_expires_at);