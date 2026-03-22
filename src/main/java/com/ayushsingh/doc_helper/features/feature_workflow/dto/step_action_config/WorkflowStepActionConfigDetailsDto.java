package com.ayushsingh.doc_helper.features.feature_workflow.dto.step_action_config;

import com.ayushsingh.doc_helper.features.feature_workflow.entity.enums.StepActionType;
import com.ayushsingh.doc_helper.features.feature_workflow.entity.enums.StepActionUiType;
import com.fasterxml.jackson.databind.JsonNode;

import jakarta.validation.constraints.NotNull;
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

    @NotNull(message = "Action type is required")
    private StepActionType actionType;

    @NotNull(message = "Action ui type is required")
    private StepActionUiType actionUiType;

    @NotNull(message = "Action uiJson is required")
    private JsonNode uiJson;
}
