package com.ayushsingh.doc_helper.features.feature_workflow.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ayushsingh.doc_helper.features.feature_workflow.entity.WorkflowStepActionConfig;

public interface WorkflowStepActionConfigRepository extends JpaRepository<WorkflowStepActionConfig, Integer> {

    Optional<WorkflowStepActionConfig> findByStepUiId(Integer stepUiId);

    List<WorkflowStepActionConfig> findAllByWorkflowStep_WorkflowStepIdOrderByStepUiIdAsc(Integer workflowStepId);
}
