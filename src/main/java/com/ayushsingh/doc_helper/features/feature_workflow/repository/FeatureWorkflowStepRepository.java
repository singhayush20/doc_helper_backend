package com.ayushsingh.doc_helper.features.feature_workflow.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ayushsingh.doc_helper.features.feature_workflow.entity.FeatureWorkflowStep;

public interface FeatureWorkflowStepRepository extends JpaRepository<FeatureWorkflowStep, Integer> {

    @EntityGraph(attributePaths = { "actions" })
    @Query("SELECT fws FROM FeatureWorkflowStep fws WHERE fws.workflowStepId = :workflowStepId")
    Optional<FeatureWorkflowStep> findStepById(@Param("workflowStepId") Integer workflowStepId);

    @EntityGraph(attributePaths = { "actions" })
    @Query("""
            SELECT fws
            FROM FeatureWorkflowStep fws
            WHERE fws.workflow.workflowId = :workflowId
            ORDER BY fws.stepOrder ASC
            """)
    List<FeatureWorkflowStep> findAllStepsByWorkflowId(@Param("workflowId") Integer workflowId);
}
