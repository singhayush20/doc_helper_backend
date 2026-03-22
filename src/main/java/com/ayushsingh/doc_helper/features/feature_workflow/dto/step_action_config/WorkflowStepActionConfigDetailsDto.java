package com.ayushsingh.doc_helper.features.feature_workflow.dto.step_action_config;

import com.ayushsingh.doc_helper.features.feature_workflow.entity.StepActionType;
import com.ayushsingh.doc_helper.features.feature_workflow.entity.StepActionUiType;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowStepActionConfigDetailsDto {
    private Integer stepUiId;
    private StepActionType actionType;
    private StepActionUiType actionUiType;
    private JsonNode uiJson;
}
