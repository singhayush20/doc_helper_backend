package com.ayushsingh.doc_helper.features.feature_workflow.service;

import java.util.List;

import com.ayushsingh.doc_helper.features.feature_workflow.dto.feature_workflow_step.FeatureWorkflowStepCreateDto;
import com.ayushsingh.doc_helper.features.feature_workflow.dto.feature_workflow_step.FeatureWorkflowStepDetailsDto;

public interface FeatureWorkflowStepService {

    FeatureWorkflowStepDetailsDto createWorkflowStep(
            Integer workflowId,
            FeatureWorkflowStepCreateDto stepCreateDto);

    FeatureWorkflowStepDetailsDto getWorkflowStepById(Integer workflowStepId);

    List<FeatureWorkflowStepDetailsDto> getWorkflowStepsByWorkflowId(Integer workflowId);
}
