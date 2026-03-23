package com.ayushsingh.doc_helper.features.feature_workflow.entity.enums;

public enum StepExecutionStatus {

    /**
     * Step created but not started yet
     */
    PENDING,

    /**
     * Waiting for user input
     */
    WAITING_FOR_INPUT,

    /**
     * Currently executing (AI / system step)
     */
    IN_PROGRESS,

    /**
     * Successfully completed
     */
    COMPLETED,

    /**
     * Execution failed (AI error, validation error, etc.)
     */
    FAILED,

    /**
     * Optional: for retries or manual intervention
     */
    RETRY
}