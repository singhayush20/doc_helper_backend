package com.ayushsingh.doc_helper.features.feature_workflow.dto.workflow;

import java.util.List;

import com.ayushsingh.doc_helper.features.feature_workflow.dto.feature_workflow_step.FeatureWorkflowStepCreateDto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowCreateDto {

    @NotBlank(message = "Workflow name is required")
    private String name;

    @NotBlank(message = "Workflow description is required")
    private String description;

    @NotNull(message = "Feature id is required")
    private Long featureId;

    @Valid
    private List<FeatureWorkflowStepCreateDto> steps;
}
