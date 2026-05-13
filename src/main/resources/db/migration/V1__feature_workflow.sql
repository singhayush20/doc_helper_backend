-- =========================
-- ENUMS
-- =========================

CREATE TYPE step_action_type AS ENUM (
    'SELECT', 'SUBMIT', 'REGENERATE', 'SHARE', 'DOWNLOAD', 'UPLOAD'
);

CREATE TYPE step_action_ui_type AS ENUM (
    'CHIP', 'TILE', 'GRID', 'BUTTON'
);

CREATE TYPE workflow_step_actor AS ENUM (
    'USER', 'SYSTEM', 'AI'
);

-- =========================
-- FEATURE WORKFLOW
-- =========================

CREATE TABLE feature_workflow (
    workflow_id SERIAL PRIMARY KEY,
    workflow_name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- =========================
-- FEATURE (ALTER)
-- =========================

ALTER TABLE features ADD COLUMN workflow_id INTEGER UNIQUE;

ALTER TABLE features
ADD CONSTRAINT fk_feature_workflow FOREIGN KEY (workflow_id) REFERENCES feature_workflow (workflow_id);

CREATE INDEX idx_features_workflow_id ON features (workflow_id);

-- =========================
-- WORKFLOW STEPS
-- =========================

CREATE TABLE feature_workflow_step (
    workflow_step_id SERIAL PRIMARY KEY,
    workflow_id INTEGER NOT NULL,
    name VARCHAR(255) NOT NULL,
    step_order INTEGER NOT NULL,
    step_actor workflow_step_actor NOT NULL,
    instruction TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_step_workflow FOREIGN KEY (workflow_id) REFERENCES feature_workflow (workflow_id) ON DELETE CASCADE
);

CREATE INDEX idx_step_workflow_id ON feature_workflow_step (workflow_id);

CREATE INDEX idx_step_order ON feature_workflow_step (workflow_id, step_order);

CREATE INDEX idx_step_actor ON feature_workflow_step (step_actor);

-- =========================
-- STEP ACTION CONFIG
-- =========================

CREATE TABLE workflow_step_action_config (
    step_ui_id SERIAL PRIMARY KEY,
    workflow_step_id INTEGER NOT NULL,
    action_type step_action_type,
    action_ui_type step_action_ui_type,
    ui_config JSONB NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_action_step FOREIGN KEY (workflow_step_id) REFERENCES feature_workflow_step (workflow_step_id) ON DELETE CASCADE
);

CREATE INDEX idx_action_step_id ON workflow_step_action_config (workflow_step_id);

CREATE INDEX idx_action_type ON workflow_step_action_config (action_type);
