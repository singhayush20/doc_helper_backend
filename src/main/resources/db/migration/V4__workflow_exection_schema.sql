CREATE TYPE workflow_execution_status AS ENUM (
    'CREATED',
    'IN_PROGRESS',
    'COMPLETED',
    'FAILED',
    'PAUSED',
    'CANCELLED'
);

CREATE TYPE step_execution_status AS ENUM (
    'PENDING',
    'WAITING_FOR_INPUT',
    'IN_PROGRESS',
    'COMPLETED',
    'FAILED',
    'RETRY'
);

CREATE TABLE workflow_execution (
    workflow_execution_id SERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    workflow_id INT NOT NULL,
    current_step_id INT,
    status workflow_execution_status NOT NULL DEFAULT 'CREATED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_execution_user_status ON workflow_execution (user_id, status);

CREATE INDEX idx_execution_workflow ON workflow_execution (workflow_id);

CREATE TABLE workflow_step_execution (
    workflow_step_execution_id SERIAL PRIMARY KEY,
    workflow_execution_id INT NOT NULL,
    step_id INT NOT NULL,
    step_order INT NOT NULL,
    input_json JSONB,
    output_json JSONB,
    status step_execution_status NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_step_execution_workflow FOREIGN KEY (workflow_execution_id) REFERENCES workflow_execution (workflow_execution_id) ON DELETE CASCADE
);

CREATE INDEX idx_step_execution_execution ON workflow_step_execution (workflow_execution_id);

CREATE INDEX idx_step_execution_order ON workflow_step_execution (
    workflow_execution_id,
    step_order
);

CREATE TABLE workflow_action_execution (
    workflow_action_execution_id SERIAL PRIMARY KEY,
    workflow_step_execution_id INT NOT NULL,
    label VARCHAR(255) NOT NULL,
    value VARCHAR(255) NOT NULL,
    metadata_json JSONB,
    selected BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_action_step_execution FOREIGN KEY (workflow_step_execution_id) REFERENCES workflow_step_execution (workflow_step_execution_id) ON DELETE CASCADE
);

CREATE INDEX idx_action_execution_step ON workflow_action_execution (workflow_step_execution_id);

CREATE INDEX idx_action_execution_selected ON workflow_action_execution (
    workflow_step_execution_id,
    selected
);