package com.ayushsingh.doc_helper.features.feature_workflow.service;

import java.util.List;

import com.ayushsingh.doc_helper.features.feature_workflow.dto.step_action_config.WorkflowStepActionConfigDetailsDto;

public interface WorkflowStepActionConfigService {

    WorkflowStepActionConfigDetailsDto createActionConfig(
            Integer workflowStepId,
            WorkflowStepActionConfigDetailsDto actionConfigDetailsDto);

    WorkflowStepActionConfigDetailsDto getActionConfigById(Integer stepUiId);

    List<WorkflowStepActionConfigDetailsDto> getActionConfigsByWorkflowStepId(Integer workflowStepId);
}
