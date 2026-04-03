ALTER TABLE workflow_step_execution
    ADD COLUMN version INTEGER;

ALTER TABLE workflow_step_execution
    ALTER COLUMN version SET NOT NULL;

CREATE UNIQUE INDEX idx_unique_step_per_execution
    ON workflow_step_execution(workflow_execution_id, step_id, version);

CREATE INDEX idx_step_execution_latest
    ON workflow_step_execution(workflow_execution_id, step_id, version DESC);