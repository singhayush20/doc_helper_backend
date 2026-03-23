package com.ayushsingh.doc_helper.features.feature_workflow.workflow_executor;

import com.ayushsingh.doc_helper.features.feature_workflow.dto.feature_workflow_step.StepExecutionResult;
import com.ayushsingh.doc_helper.features.feature_workflow.dto.step_action_config.UserActionRequest;

public interface WorkflowExecutor {

    StepExecutionResult startWorkflow(Integer workflowId, Long userId);

    StepExecutionResult executeNextStep(Integer executionId);

    StepExecutionResult handleUserAction(Integer executionId,
            UserActionRequest request);
}