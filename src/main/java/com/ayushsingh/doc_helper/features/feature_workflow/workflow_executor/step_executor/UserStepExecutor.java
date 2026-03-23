package com.ayushsingh.doc_helper.features.feature_workflow.workflow_executor.step_executor;

import java.util.List;

import org.springframework.stereotype.Component;

import com.ayushsingh.doc_helper.features.feature_workflow.dto.feature_workflow_step.StepExecutionResult;
import com.ayushsingh.doc_helper.features.feature_workflow.dto.step_action_config.ActionDto;
import com.ayushsingh.doc_helper.features.feature_workflow.dto.workflow.WorkflowExecutionContext;
import com.ayushsingh.doc_helper.features.feature_workflow.entity.FeatureWorkflowStep;
import com.ayushsingh.doc_helper.features.feature_workflow.entity.WorkflowActionExecution;
import com.ayushsingh.doc_helper.features.feature_workflow.entity.WorkflowExecution;
import com.ayushsingh.doc_helper.features.feature_workflow.entity.WorkflowStepExecution;
import com.ayushsingh.doc_helper.features.feature_workflow.repository.WorkflowActionExecutionRepository;
import com.ayushsingh.doc_helper.features.feature_workflow.workflow_executor.StepExecutor;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserStepExecutor implements StepExecutor {

    private final WorkflowActionExecutionRepository actionExecutionRepository;

    @Override
    public StepExecutionResult execute(
            FeatureWorkflowStep step,
            WorkflowExecution execution,
            WorkflowExecutionContext context) {

        // Fetch already generated actions
        var stepExecution = (WorkflowStepExecution) context.get("currentStepExecution");

        List<WorkflowActionExecution> actions = actionExecutionRepository.findByStepExecution(stepExecution);

        List<ActionDto> dto = actions.stream()
                .map(a -> ActionDto.builder()
                        .label(a.getLabel())
                        .value(a.getValue())
                        .selected(a.getSelected())
                        .build())
                .toList();

        return StepExecutionResult.builder()
                .stepId(step.getWorkflowStepId())
                .stepName(step.getName())
                .instruction(step.getInstruction())
                .actions(dto)
                .status("WAITING_FOR_INPUT")
                .build();
    }
}