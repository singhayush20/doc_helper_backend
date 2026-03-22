package com.ayushsingh.doc_helper.features.feature_workflow.dto.workflow;

import java.util.List;

import com.ayushsingh.doc_helper.features.feature_workflow.dto.feature_workflow_step.FeatureWorkflowStepCreateDto;

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
    private String name;
    private String description;
    private Long featureId;
    private List<FeatureWorkflowStepCreateDto> steps;
}
