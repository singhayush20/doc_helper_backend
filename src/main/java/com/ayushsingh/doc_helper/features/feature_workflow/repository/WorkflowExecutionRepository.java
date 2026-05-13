package com.ayushsingh.doc_helper.features.feature_workflow.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ayushsingh.doc_helper.features.feature_workflow.entity.WorkflowExecution;
import com.ayushsingh.doc_helper.features.feature_workflow.entity.enums.WorkflowExecutionStatus;

public interface WorkflowExecutionRepository
        extends JpaRepository<WorkflowExecution, Integer> {

    /**
     * Used for resume functionality
     */
    Optional<WorkflowExecution> findFirstByUserIdAndStatusOrderByCreatedAtDesc(
            Long userId,
            WorkflowExecutionStatus status);
}
