package com.ayushsingh.doc_helper.features.feature_workflow.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ayushsingh.doc_helper.features.feature_workflow.entity.FeatureWorkflowStep;

public interface FeatureWorkflowStepRepository extends JpaRepository<FeatureWorkflowStep, Integer> {

    @EntityGraph(attributePaths = { "actions" })
    Optional<FeatureWorkflowStep> findByWorkflowStepId(Integer workflowStepId);

    @EntityGraph(attributePaths = { "actions" })
    List<FeatureWorkflowStep> findAllByWorkflow_WorkflowIdOrderByStepOrderAsc(Integer workflowId);
}
