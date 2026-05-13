package com.ayushsingh.doc_helper.features.feature_workflow.repository;

import com.ayushsingh.doc_helper.features.feature_workflow.entity.WorkflowExecution;
import com.ayushsingh.doc_helper.features.feature_workflow.entity.WorkflowStepExecution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkflowStepExecutionRepository
        extends JpaRepository<WorkflowStepExecution, Integer> {

    /**
     * Get latest step for execution (resume)
     */
    Optional<WorkflowStepExecution> findTopByWorkflowExecution_WorkflowExecutionIdOrderByStepOrderDesc(
            Integer workflowExecutionId);

    /**
     * Idempotency check (important)
     */
    Optional<WorkflowStepExecution> findByWorkflowExecution_WorkflowExecutionIdAndStepId(
            Integer executionId,
            Integer stepId);

    /**
     * Load all steps for context building
     */
    List<WorkflowStepExecution> findByWorkflowExecution(WorkflowExecution workflowExecution);

    Optional<WorkflowStepExecution>
    findTopByWorkflowExecution_WorkflowExecutionIdAndStepIdOrderByVersionDesc(
            Integer executionId,
            Integer stepId
    );
}