package com.ayushsingh.doc_helper.features.feature_workflow.workflow_executor.workflow_executor;

import com.ayushsingh.doc_helper.core.exception_handling.ExceptionCodes;
import com.ayushsingh.doc_helper.core.exception_handling.exceptions.BaseException;
import com.ayushsingh.doc_helper.features.feature_workflow.dto.feature_workflow_step.StepExecutionResult;
import com.ayushsingh.doc_helper.features.feature_workflow.dto.step_action_config.ActionDto;
import com.ayushsingh.doc_helper.features.feature_workflow.dto.step_action_config.UserActionRequest;
import com.ayushsingh.doc_helper.features.feature_workflow.dto.workflow.WorkflowExecutionContext;
import com.ayushsingh.doc_helper.features.feature_workflow.entity.FeatureWorkflowStep;
import com.ayushsingh.doc_helper.features.feature_workflow.entity.WorkflowActionExecution;
import com.ayushsingh.doc_helper.features.feature_workflow.entity.WorkflowExecution;
import com.ayushsingh.doc_helper.features.feature_workflow.entity.WorkflowStepExecution;
import com.ayushsingh.doc_helper.features.feature_workflow.entity.enums.StepExecutionStatus;
import com.ayushsingh.doc_helper.features.feature_workflow.entity.enums.WorkflowExecutionStatus;
import com.ayushsingh.doc_helper.features.feature_workflow.repository.*;
import com.ayushsingh.doc_helper.features.feature_workflow.workflow_executor.StepExecutor;
import com.ayushsingh.doc_helper.features.feature_workflow.workflow_executor.step_executor.StepExecutorFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkflowExecutionTransactionalService {

    private final FeatureWorkflowRepository workflowRepository;
    private final FeatureWorkflowStepRepository stepRepository;
    private final WorkflowExecutionRepository executionRepository;
    private final WorkflowStepExecutionRepository stepExecutionRepository;
    private final WorkflowActionExecutionRepository actionExecutionRepository;
    private final StepExecutorFactory stepExecutorFactory;

    // =========================================================
    // CREATE WORKFLOW
    // =========================================================

    public WorkflowExecution createWorkflowExecution(Integer workflowId, Long userId) {

        if (!workflowRepository.existsById(workflowId)) {
            throw new BaseException("Workflow not found", ExceptionCodes.WORKFLOW_NOT_FOUND);
        }

        FeatureWorkflowStep firstStep =
                stepRepository.findFirstByWorkflow_WorkflowIdOrderByStepOrderAsc(workflowId)
                        .orElseThrow(() -> new BaseException("No workflow steps found",
                                ExceptionCodes.WORKFLOW_STEP_NOT_FOUND));

        WorkflowExecution execution = WorkflowExecution.builder()
                .workflowId(workflowId)
                .userId(userId)
                .currentStepId(firstStep.getWorkflowStepId())
                .status(WorkflowExecutionStatus.IN_PROGRESS)
                .build();

        return executionRepository.saveAndFlush(execution);
    }

    // =========================================================
    // EXECUTE CURRENT STEP
    // =========================================================

    public StepExecutionResult executeCurrentStep(Integer executionId) {

        WorkflowExecution execution = loadExecutionOrThrow(executionId);
        FeatureWorkflowStep step = loadStepOrThrow(execution.getCurrentStepId());

        return executeStep(step, execution, false);
    }

    // =========================================================
    // HANDLE USER ACTION
    // =========================================================

    public StepExecutionResult handleUserAction(Integer executionId,
                                                UserActionRequest request) {

        WorkflowExecution execution = loadExecutionOrThrow(executionId);

        WorkflowStepExecution stepExecution =
                stepExecutionRepository
                        .findTopByWorkflowExecution_WorkflowExecutionIdOrderByStepOrderDesc(executionId)
                        .orElseThrow(() -> new BaseException("Workflow execution step not found",
                                ExceptionCodes.EXECUTION_STEP_NOT_FOUND));

        // Extract flag FIRST
        boolean reExecute = Boolean.TRUE.equals(request.getReExecute());

        // =====================================================
        // CASE 1: RE-EXECUTE CURRENT STEP
        // =====================================================

        if (reExecute) {

            FeatureWorkflowStep currentStep =
                    loadStepOrThrow(stepExecution.getStepId());

            // DO NOT mark previous as completed again
            // DO NOT move to next step

            return executeStep(currentStep, execution, true);
        }

        // =====================================================
        // CASE 2: NORMAL USER ACTION
        // =====================================================

        WorkflowActionExecution action =
                actionExecutionRepository
                        .findByStepExecutionAndValue(stepExecution, request.getActionValue())
                        .orElseThrow(() -> new BaseException("Workflow " +
                                "execution step action not found",
                                ExceptionCodes.WORKFLOW_STEP_ACTION_NOT_FOUND));

        action.setSelected(true);

        if (request.getInputJson() != null) {
            stepExecution.setInputJson(request.getInputJson());
        }

        stepExecution.setStatus(StepExecutionStatus.COMPLETED);
        stepExecutionRepository.save(stepExecution);

        // Move to next step
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

        return executeStep(nextExecutionStep, execution, false);
    }

    private StepExecutionResult executeStep(FeatureWorkflowStep step,
                                            WorkflowExecution execution,
                                            boolean reExecute) {

        Optional<WorkflowStepExecution> latest =
                stepExecutionRepository
                        .findTopByWorkflowExecution_WorkflowExecutionIdAndStepIdOrderByVersionDesc(
                                execution.getWorkflowExecutionId(),
                                step.getWorkflowStepId());

        if (!reExecute && latest.isPresent()) {
            return buildResponseFromExecution(latest.get());
        }

        Integer version = getNextVersion(
                execution.getWorkflowExecutionId(),
                step.getWorkflowStepId());

        WorkflowStepExecution stepExecution;

        try {
            stepExecution = WorkflowStepExecution.builder()
                    .workflowExecution(execution)
                    .stepId(step.getWorkflowStepId())
                    .stepOrder(step.getStepOrder())
                    .version(version)
                    .status(StepExecutionStatus.IN_PROGRESS)
                    .build();

            stepExecutionRepository.saveAndFlush(stepExecution);

        } catch (DataIntegrityViolationException ex) {

            // Race condition fallback
            WorkflowStepExecution existing =
                    stepExecutionRepository
                            .findTopByWorkflowExecution_WorkflowExecutionIdAndStepIdOrderByVersionDesc(
                                    execution.getWorkflowExecutionId(),
                                    step.getWorkflowStepId())
                            .orElseThrow();

            return buildResponseFromExecution(existing);
        }

        WorkflowExecutionContext context = buildContext(execution);
        context.put("currentStepExecution", stepExecution);

        StepExecutor executor =
                stepExecutorFactory.getExecutor(step.getStepActor());

        return executor.execute(step, execution, context);
    }

    // =========================================================
    // VERSION CALCULATION
    // =========================================================

    private Integer getNextVersion(Integer executionId, Integer stepId) {

        return stepExecutionRepository
                .findTopByWorkflowExecution_WorkflowExecutionIdAndStepIdOrderByVersionDesc(
                        executionId, stepId)
                .map(e -> e.getVersion() + 1)
                .orElse(1);
    }

    // =========================================================
    // CONTEXT
    // =========================================================

    private WorkflowExecutionContext buildContext(WorkflowExecution execution) {

        List<WorkflowStepExecution> steps =
                stepExecutionRepository.findByWorkflowExecution(execution);

        Map<String, Object> data = new HashMap<>();

        for (WorkflowStepExecution step : steps) {
            data.put("step_" + step.getStepOrder(), step.getOutputJson());
        }

        return WorkflowExecutionContext.builder()
                .executionId(execution.getWorkflowExecutionId())
                .data(data)
                .build();
    }

    // =========================================================
    // RESPONSE BUILDERS
    // =========================================================

    private StepExecutionResult buildResponseFromExecution(
            WorkflowStepExecution stepExecution) {

        List<WorkflowActionExecution> actions =
                actionExecutionRepository.findByStepExecution(stepExecution);

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

    // =========================================================
    // LOAD HELPERS
    // =========================================================

    private WorkflowExecution loadExecutionOrThrow(Integer executionId) {
        return executionRepository.findById(executionId)
                .orElseThrow(() -> new BaseException("Workflow execution not found",
                        ExceptionCodes.WORKFLOW_EXECUTION_NOT_FOUND));
    }

    private FeatureWorkflowStep loadStepOrThrow(Integer stepId) {
        return stepRepository.findById(stepId)
                .orElseThrow(() -> new BaseException("Workflow step not found",
                        ExceptionCodes.WORKFLOW_STEP_NOT_FOUND));
    }
}