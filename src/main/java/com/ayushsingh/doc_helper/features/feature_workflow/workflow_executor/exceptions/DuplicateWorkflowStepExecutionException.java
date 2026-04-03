package com.ayushsingh.doc_helper.features.feature_workflow.workflow_executor.exceptions;

import lombok.Getter;

@Getter
public class DuplicateWorkflowStepExecutionException extends RuntimeException {

    private final Integer executionId;
    private final Integer stepId;

    public DuplicateWorkflowStepExecutionException(Integer executionId,
                                                   Integer stepId,
                                                   Throwable cause) {
        super("Duplicate workflow step execution detected", cause);
        this.executionId = executionId;
        this.stepId = stepId;
    }
}
