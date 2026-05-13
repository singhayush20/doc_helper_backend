package com.ayushsingh.doc_helper.features.feature_workflow.workflow_executor.step_executor;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.ayushsingh.doc_helper.core.utils.JsonUtils;
import com.ayushsingh.doc_helper.features.feature_workflow.dto.feature_workflow_step.StepExecutionResult;
import com.ayushsingh.doc_helper.features.feature_workflow.dto.step_action_config.ActionDto;
import com.ayushsingh.doc_helper.features.feature_workflow.dto.workflow.WorkflowExecutionContext;
import com.ayushsingh.doc_helper.features.feature_workflow.dto_to_entity_mapper.ActionMapper;
import com.ayushsingh.doc_helper.features.feature_workflow.entity.FeatureWorkflowStep;
import com.ayushsingh.doc_helper.features.feature_workflow.entity.WorkflowActionExecution;
import com.ayushsingh.doc_helper.features.feature_workflow.entity.WorkflowExecution;
import com.ayushsingh.doc_helper.features.feature_workflow.entity.WorkflowStepActionConfig;
import com.ayushsingh.doc_helper.features.feature_workflow.entity.WorkflowStepExecution;
import com.ayushsingh.doc_helper.features.feature_workflow.repository.WorkflowActionExecutionRepository;
import com.ayushsingh.doc_helper.features.feature_workflow.repository.WorkflowStepActionConfigRepository;
import com.ayushsingh.doc_helper.features.feature_workflow.workflow_executor.ActionProvider;
import com.ayushsingh.doc_helper.features.feature_workflow.workflow_executor.ActionProviderRegistry;
import com.ayushsingh.doc_helper.features.feature_workflow.workflow_executor.StepExecutor;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SystemStepExecutor implements StepExecutor {

    private final WorkflowStepActionConfigRepository actionConfigRepository;
    private final WorkflowActionExecutionRepository actionExecutionRepository;
    private final ActionProviderRegistry providerRegistry;

    @Override
    public StepExecutionResult execute(
            FeatureWorkflowStep step,
            WorkflowExecution execution,
            WorkflowExecutionContext context) {

        var stepExecution = (WorkflowStepExecution) context.get("currentStepExecution");

        List<WorkflowStepActionConfig> configs = actionConfigRepository.findByWorkflowStep(step);

        List<ActionDto> actions = new ArrayList<>();

        for (WorkflowStepActionConfig config : configs) {

            if (Boolean.TRUE.equals(config.getIsDynamic())) {

                ActionProvider provider = providerRegistry.get(config.getActionProvider());

                actions.addAll(provider.resolve(context));

            } else {
                actions.add(ActionMapper.fromConfig(config));
            }
        }

        // Persist
        List<WorkflowActionExecution> entities = actions.stream()
                .map((ActionDto action) -> WorkflowActionExecution.builder()
                        .stepExecution(stepExecution)
                        .label(action.getLabel())
                        .value(action.getValue())
                        .metadataJson(JsonUtils.toJson(action.getMetadata()))
                        .selected(false)
                        .build())
                .toList();

        actionExecutionRepository.saveAll(entities);

        return StepExecutionResult.builder()
                .stepId(step.getWorkflowStepId())
                .stepName(step.getName())
                .instruction(step.getInstruction())
                .actions(actions)
                .status("WAITING_FOR_INPUT")
                .build();
    }
}