package com.ayushsingh.doc_helper.features.feature_workflow.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ayushsingh.doc_helper.features.feature_workflow.entity.WorkflowStepActionConfig;

public interface WorkflowStepActionConfigRepository extends JpaRepository<WorkflowStepActionConfig, Integer> {

    @Query("SELECT wsac FROM WorkflowStepActionConfig wsac WHERE wsac.stepUiId = :stepUiId")
    Optional<WorkflowStepActionConfig> findActionConfigById(@Param("stepUiId") Integer stepUiId);

    @Query("""
            SELECT wsac
            FROM WorkflowStepActionConfig wsac
            WHERE wsac.workflowStep.workflowStepId = :workflowStepId
            ORDER BY wsac.stepUiId ASC
            """)
    List<WorkflowStepActionConfig> findAllActionConfigsByWorkflowStepId(@Param("workflowStepId") Integer workflowStepId);
}
