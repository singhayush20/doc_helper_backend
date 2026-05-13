package com.ayushsingh.doc_helper.features.feature_workflow.workflow_executor.step_executor;

import org.springframework.stereotype.Component;

import com.ayushsingh.doc_helper.features.feature_workflow.entity.enums.WorkflowStepActor;
import com.ayushsingh.doc_helper.features.feature_workflow.workflow_executor.StepExecutor;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class StepExecutorFactory {

    private final UserStepExecutor userStepExecutor;
    private final SystemStepExecutor systemStepExecutor;
    private final AiStepExecutor aiStepExecutor;

    public StepExecutor getExecutor(WorkflowStepActor actor) {

        return switch (actor) {
            case USER -> userStepExecutor;
            case SYSTEM -> systemStepExecutor;
            case AI -> aiStepExecutor;
        };
    }
}