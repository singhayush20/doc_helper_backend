package com.ayushsingh.doc_helper.features.feature_workflow.workflow_executor.workflow_executor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.ayushsingh.doc_helper.core.exception_handling.ExceptionCodes;
import com.ayushsingh.doc_helper.core.exception_handling.exceptions.BaseException;
import com.ayushsingh.doc_helper.core.utils.JsonUtils;
import com.ayushsingh.doc_helper.features.feature_workflow.dto.feature_workflow_step.StepExecutionResult;
import com.ayushsingh.doc_helper.features.feature_workflow.dto.step_action_config.ActionDto;
import com.ayushsingh.doc_helper.features.feature_workflow.dto.step_action_config.UserActionRequest;
import com.ayushsingh.doc_helper.features.feature_workflow.dto.workflow.WorkflowExecutionContext;
import com.ayushsingh.doc_helper.features.feature_workflow.dto_to_entity_mapper.ActionMapper;
import com.ayushsingh.doc_helper.features.feature_workflow.entity.FeatureWorkflowStep;
import com.ayushsingh.doc_helper.features.feature_workflow.entity.WorkflowActionExecution;
import com.ayushsingh.doc_helper.features.feature_workflow.entity.WorkflowExecution;
import com.ayushsingh.doc_helper.features.feature_workflow.entity.WorkflowStepActionConfig;
import com.ayushsingh.doc_helper.features.feature_workflow.entity.WorkflowStepExecution;
import com.ayushsingh.doc_helper.features.feature_workflow.entity.enums.StepExecutionStatus;
import com.ayushsingh.doc_helper.features.feature_workflow.entity.enums.WorkflowExecutionStatus;
import com.ayushsingh.doc_helper.features.feature_workflow.repository.FeatureWorkflowRepository;
import com.ayushsingh.doc_helper.features.feature_workflow.repository.FeatureWorkflowStepRepository;
import com.ayushsingh.doc_helper.features.feature_workflow.repository.WorkflowActionExecutionRepository;
import com.ayushsingh.doc_helper.features.feature_workflow.repository.WorkflowExecutionRepository;
import com.ayushsingh.doc_helper.features.feature_workflow.repository.WorkflowStepActionConfigRepository;
import com.ayushsingh.doc_helper.features.feature_workflow.repository.WorkflowStepExecutionRepository;
import com.ayushsingh.doc_helper.features.feature_workflow.workflow_executor.ActionProvider;
import com.ayushsingh.doc_helper.features.feature_workflow.workflow_executor.ActionProviderRegistry;
import com.ayushsingh.doc_helper.features.feature_workflow.workflow_executor.WorkflowExecutor;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@AllArgsConstructor
public class WorkflowExecutorImpl implements WorkflowExecutor {
        private final FeatureWorkflowRepository workflowRepository;
        private final FeatureWorkflowStepRepository stepRepository;
        private final WorkflowExecutionRepository executionRepository;
        private final WorkflowStepExecutionRepository stepExecutionRepository;
        private final WorkflowActionExecutionRepository actionExecutionRepository;
        private final WorkflowStepActionConfigRepository actionConfigRepository;

        private final ActionProviderRegistry providerRegistry;

        @Override
        public StepExecutionResult startWorkflow(Integer workflowId, Long userId) {

                var isWorkflowPresent = workflowRepository.existsById(workflowId);

                if (!isWorkflowPresent) {
                        throw new BaseException("Workflow not present", ExceptionCodes.WORKFLOW_NOT_FOUND);
                }

                var firstStep = stepRepository.findFirstByWorkflowIdOrderByStepOrderAsc(workflowId);

                if (firstStep != null && firstStep.isPresent()) {
                        var workflowStep = firstStep.get();
                        WorkflowExecution execution = WorkflowExecution.builder()
                                        .workflowId(workflowId)
                                        .userId(userId)
                                        .currentStepId(workflowStep.getWorkflowStepId())
                                        .status(WorkflowExecutionStatus.IN_PROGRESS)
                                        .build();

                        executionRepository.save(execution);

                        return executeStep(workflowStep, execution);
                }

                throw new BaseException("Execution step not found", ExceptionCodes.EXECUTION_STEP_NOT_FOUND);
        }

        @Override
        public StepExecutionResult executeNextStep(Integer executionId) {

                WorkflowExecution execution = executionRepository.findById(executionId)
                                .orElseThrow();

                FeatureWorkflowStep step = stepRepository.findById(execution.getCurrentStepId())
                                .orElseThrow();

                return executeStep(step, execution);
        }

        @Override
        public StepExecutionResult handleUserAction(Integer executionId,
                        UserActionRequest request) {

                WorkflowExecution execution = executionRepository.findById(executionId)
                                .orElseThrow();

                WorkflowStepExecution stepExecution = stepExecutionRepository
                                .findTopByWorkflowExecution_WorkflowExecutionIdOrderByStepOrderDesc(executionId)
                                .orElseThrow();

                // 1. Mark selected action
                WorkflowActionExecution action = actionExecutionRepository.findByStepExecutionAndValue(
                                stepExecution, request.getActionValue())
                                .orElseThrow();

                action.setSelected(true);
                actionExecutionRepository.save(action);

                // 2. Save input if present
                if (request.getInputJson() != null) {
                        stepExecution.setInputJson(request.getInputJson());
                }

                stepExecution.setStatus(StepExecutionStatus.COMPLETED);
                stepExecutionRepository.save(stepExecution);

                // 3. Move to next step
                var nextStep = stepRepository.findNextStep(
                                execution.getWorkflowId(),
                                stepExecution.getStepOrder());

                if (nextStep == null || nextStep.isEmpty()) {
                        execution.setStatus(WorkflowExecutionStatus.COMPLETED);
                        return buildFinalResponse();
                }
                var nextExecutionStep = nextStep.get();
                execution.setCurrentStepId(nextExecutionStep.getWorkflowStepId());
                executionRepository.save(execution);

                return executeStep(nextExecutionStep, execution);
        }

        private StepExecutionResult executeStep(FeatureWorkflowStep step,
                        WorkflowExecution execution) {

                // 1. Check if already executed (resume case)
                Optional<WorkflowStepExecution> existing = stepExecutionRepository
                                .findByWorkflowExecution_WorkflowExecutionIdAndStepId(
                                                execution.getWorkflowExecutionId(),
                                                step.getWorkflowStepId());

                if (existing.isPresent()) {
                        return buildResponseFromExecution(existing.get());
                }

                // 2. Create step execution
                WorkflowStepExecution stepExecution = WorkflowStepExecution.builder()
                                .workflowExecution(execution)
                                .stepId(step.getWorkflowStepId())
                                .stepOrder(step.getStepOrder())
                                .status(StepExecutionStatus.IN_PROGRESS)
                                .build();

                stepExecutionRepository.save(stepExecution);

                // 3. Build context
                WorkflowExecutionContext context = buildContext(execution);

                // 4. Resolve actions
                List<ActionDto> actions = resolveActions(step, context);

                // 5. Persist actions
                saveActions(stepExecution, actions);

                // 6. Update status
                stepExecution.setStatus(StepExecutionStatus.WAITING_FOR_INPUT);
                stepExecutionRepository.save(stepExecution);

                return buildResponse(step, actions);
        }
    
        private List<ActionDto> resolveActions(FeatureWorkflowStep step,
                        WorkflowExecutionContext context) {

                List<WorkflowStepActionConfig> configs = actionConfigRepository.findByWorkflowStep(step);

                List<ActionDto> result = new ArrayList<>();

                for (WorkflowStepActionConfig config : configs) {

                        if (Boolean.TRUE.equals(config.getIsDynamic())) {

                                ActionProvider provider = providerRegistry.get(config.getActionProvider());

                                result.addAll(provider.resolve(context));

                        } else {

                                result.add(ActionMapper.fromConfig(config));
                        }
                }

                return result;
        }

        private void saveActions(WorkflowStepExecution stepExecution,
                        List<ActionDto> actions) {

                List<WorkflowActionExecution> entities = actions.stream()
                                .map(a -> WorkflowActionExecution.builder()
                                                .stepExecution(stepExecution)
                                                .label(a.getLabel())
                                                .value(a.getValue())
                                                .metadataJson(JsonUtils.toJson(a.getMetadata()))
                                                .selected(Boolean.FALSE)
                                                .build())
                                .toList();

                actionExecutionRepository.saveAll(entities);
        }

        private StepExecutionResult buildResponse(FeatureWorkflowStep step,
                        List<ActionDto> actions) {

                return StepExecutionResult.builder()
                                .stepId(step.getWorkflowStepId())
                                .stepName(step.getName())
                                .stepOrder(step.getStepOrder())
                                .instruction(step.getInstruction())
                                .actions(actions)
                                .status("WAITING_FOR_INPUT")
                                .build();
        }

        private WorkflowExecutionContext buildContext(WorkflowExecution execution) {

                List<WorkflowStepExecution> steps = stepExecutionRepository.findByWorkflowExecution(execution);

                Map<String, Object> data = new HashMap<>();

                for (WorkflowStepExecution step : steps) {
                        data.put("step_" + step.getStepOrder(), step.getOutputJson());
                }

                return WorkflowExecutionContext.builder()
                                .executionId(execution.getWorkflowExecutionId())
                                .data(data)
                                .build();
        }

        private StepExecutionResult buildResponseFromExecution(
                        WorkflowStepExecution stepExecution) {

                List<WorkflowActionExecution> actions = actionExecutionRepository.findByStepExecution(stepExecution);

                List<ActionDto> dto = actions.stream()
                                .map(a -> ActionDto.builder()
                                                .label(a.getLabel())
                                                .value(a.getValue())
                                                .selected(a.getSelected())
                                                .build())
                                .toList();

                return StepExecutionResult.builder()
                                .stepId(stepExecution.getStepId())
                                .actions(dto)
                                .status(stepExecution.getStatus().name())
                                .build();
        }

        private StepExecutionResult buildFinalResponse() {

                return StepExecutionResult.builder()
                                .isTerminal(true)
                                .stepName("Completed")
                                .instruction("Workflow completed successfully")
                                .actions(Collections.emptyList())
                                .status("COMPLETED")
                                .build();
        }
}
