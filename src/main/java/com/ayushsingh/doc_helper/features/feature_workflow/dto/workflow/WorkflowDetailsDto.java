package com.ayushsingh.doc_helper.features.feature_workflow.dto.workflow;

import java.util.List;

import com.ayushsingh.doc_helper.features.feature_workflow.dto.feature_workflow_step.FeatureWorkflowStepDetailsDto;

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
public class WorkflowDetailsDto {
    private Integer workflowId;
    private String name;
    private String description;
    private List<FeatureWorkflowStepDetailsDto> steps;
}
