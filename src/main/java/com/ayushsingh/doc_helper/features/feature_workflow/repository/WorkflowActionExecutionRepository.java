package com.ayushsingh.doc_helper.features.feature_workflow.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ayushsingh.doc_helper.features.feature_workflow.entity.WorkflowActionExecution;
import com.ayushsingh.doc_helper.features.feature_workflow.entity.WorkflowStepExecution;

public interface WorkflowActionExecutionRepository
        extends JpaRepository<WorkflowActionExecution, Integer> {

    /**
     * Get all actions for a step
     */
    List<WorkflowActionExecution> findByStepExecution(WorkflowStepExecution stepExecution);

    /**
     * Find specific action (for user selection)
     */
    Optional<WorkflowActionExecution> findByStepExecutionAndValue(
            WorkflowStepExecution stepExecution,
            String value);

    /**
     * Get selected action (useful for validation)
     */
    Optional<WorkflowActionExecution> findByStepExecutionAndSelectedTrue(
            WorkflowStepExecution stepExecution);
}