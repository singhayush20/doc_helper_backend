package com.ayushsingh.doc_helper.features.feature_workflow.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ayushsingh.doc_helper.features.feature_workflow.entity.FeatureWorkflow;

public interface FeatureWorkflowRepository extends JpaRepository<FeatureWorkflow, Integer> {

    boolean existsByName(String name);

    @EntityGraph(attributePaths = { "steps", "steps.actions" })
    Optional<FeatureWorkflow> findByWorkflowId(Integer workflowId);

    @EntityGraph(attributePaths = { "steps", "steps.actions" })
    List<FeatureWorkflow> findAllByOrderByCreatedAtDesc();
}
