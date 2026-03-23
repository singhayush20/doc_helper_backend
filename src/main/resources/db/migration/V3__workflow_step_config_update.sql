ALTER TABLE workflow_step_action_config
RENAME COLUMN ui_config TO ui_schema;

ALTER TABLE workflow_step_action_config
ADD COLUMN is_dynamic BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE workflow_step_action_config
ADD COLUMN action_provider VARCHAR(100);

CREATE INDEX idx_action_provider ON workflow_step_action_config (action_provider);