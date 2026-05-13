package com.ayushsingh.doc_helper.features.feature_workflow.workflow_executor;

import java.util.List;

import com.ayushsingh.doc_helper.features.feature_workflow.dto.step_action_config.ActionDto;
import com.ayushsingh.doc_helper.features.feature_workflow.dto.workflow.WorkflowExecutionContext;

public interface ActionProvider {
    List<ActionDto> resolve(WorkflowExecutionContext context);
}
