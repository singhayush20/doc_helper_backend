package com.ayushsingh.doc_helper.features.feature_workflow.workflow_executor;

import com.ayushsingh.doc_helper.features.feature_workflow.dto.feature_workflow_step.StepExecutionResult;
import com.ayushsingh.doc_helper.features.feature_workflow.dto.workflow.WorkflowExecutionContext;
import com.ayushsingh.doc_helper.features.feature_workflow.entity.FeatureWorkflowStep;
import com.ayushsingh.doc_helper.features.feature_workflow.entity.WorkflowExecution;

public interface StepExecutor {

    StepExecutionResult execute(
            FeatureWorkflowStep step,
            WorkflowExecution execution,
            WorkflowExecutionContext context);
}
