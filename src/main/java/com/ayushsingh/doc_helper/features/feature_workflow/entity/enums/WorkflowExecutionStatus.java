package com.ayushsingh.doc_helper.features.feature_workflow.entity.enums;

public enum WorkflowExecutionStatus {

    /**
     * Workflow created but not started yet
     */
    CREATED,

    /**
     * Workflow is currently running
     */
    IN_PROGRESS,

    /**
     * Workflow completed successfully
     */
    COMPLETED,

    /**
     * Workflow failed at some step
     */
    FAILED,

    /**
     * Workflow paused (user left, resumable)
     */
    PAUSED,

    /**
     * Workflow cancelled by user/system
     */
    CANCELLED
}
