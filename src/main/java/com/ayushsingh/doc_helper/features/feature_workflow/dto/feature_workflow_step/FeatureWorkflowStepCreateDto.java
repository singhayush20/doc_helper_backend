package com.ayushsingh.doc_helper.features.feature_workflow.dto.feature_workflow_step;

import java.util.List;

import com.ayushsingh.doc_helper.features.feature_workflow.dto.step_action_config.WorkflowStepActionConfigDetailsDto;
import com.ayushsingh.doc_helper.features.feature_workflow.entity.WorkflowStepActor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
public class FeatureWorkflowStepCreateDto {

    @NotBlank(message = "Step name is required")
    private String name;

    @NotNull(message = "Step order is required")
    private Integer stepOrder;

    @NotNull(message = "Step actor is required")
    private WorkflowStepActor stepActor;

    private String instruction;

    @Valid
    private List<WorkflowStepActionConfigDetailsDto> actions;
}
