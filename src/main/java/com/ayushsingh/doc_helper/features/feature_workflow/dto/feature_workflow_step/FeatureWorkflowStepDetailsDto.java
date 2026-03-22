package com.ayushsingh.doc_helper.features.feature_workflow.dto.feature_workflow_step;

import java.util.List;

import com.ayushsingh.doc_helper.features.feature_workflow.dto.step_action_config.WorkflowStepActionConfigDetailsDto;
import com.ayushsingh.doc_helper.features.feature_workflow.entity.WorkflowStepActor;

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
public class FeatureWorkflowStepDetailsDto {
    private Integer workflowStepId;
    private String name;
    private Integer stepOrder;
    private WorkflowStepActor stepActor;
    private String instruction;
    private List<WorkflowStepActionConfigDetailsDto> actions;
}
