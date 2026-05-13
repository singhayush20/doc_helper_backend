package com.ayushsingh.doc_helper.features.feature_workflow.service;

import java.util.List;

import com.ayushsingh.doc_helper.features.feature_workflow.dto.workflow.WorkflowCreateDto;
import com.ayushsingh.doc_helper.features.feature_workflow.dto.workflow.WorkflowDetailsDto;

public interface FeatureWorkflowService {

    WorkflowDetailsDto createWorkflow(WorkflowCreateDto workflowCreateDto);

    WorkflowDetailsDto getWorkflowById(Integer workflowId);

    List<WorkflowDetailsDto> getAllWorkflows();

    void deleteWorkflow(Integer workflowId);
}
