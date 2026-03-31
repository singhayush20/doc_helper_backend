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
import com.ayushsingh.doc_helper.features.feature_workflow.entity.enums.WorkflowStepActor;
import com.ayushsingh.doc_helper.features.feature_workflow.repository.*;
import com.ayushsingh.doc_helper.features.feature_workflow.workflow_executor.StepExecutor;
import com.ayushsingh.doc_helper.features.feature_workflow.workflow_executor.WorkflowExecutor;
import com.ayushsingh.doc_helper.features.feature_workflow.workflow_executor.step_executor.StepExecutorFactory;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkflowExecutorImpl implements WorkflowExecutor {

    private final FeatureWorkflowRepository workflowRepository;
    private final FeatureWorkflowStepRepository stepRepository;

    private final WorkflowExecutionRepository executionRepository;
    private final WorkflowStepExecutionRepository stepExecutionRepository;
    private final WorkflowActionExecutionRepository actionExecutionRepository;

    private final StepExecutorFactory stepExecutorFactory;

    // =========================================================
    // START WORKFLOW
    // =========================================================

    @Override
    public StepExecutionResult startWorkflow(Integer workflowId, Long userId) {

        var isWorkflowPresent = workflowRepository.existsById(workflowId);

        if (isWorkflowPresent) {
            FeatureWorkflowStep firstStep =
                    stepRepository.findFirstByWorkflowIdOrderByStepOrderAsc(workflowId)
                            .orElseThrow(() -> new BaseException("No workflow steps found",
                                    ExceptionCodes.WORKFLOW_STEP_NOT_FOUND));

            WorkflowExecution execution = WorkflowExecution.builder()
                    .workflowId(workflowId)
                    .userId(userId)
                    .currentStepId(firstStep.getWorkflowStepId())
                    .status(WorkflowExecutionStatus.IN_PROGRESS)
                    .build();

            executionRepository.save(execution);

            return executeStep(firstStep, execution);
        } else {
            throw new BaseException("Workflow not found",
                    ExceptionCodes.WORKFLOW_NOT_FOUND);
        }
    }

    // =========================================================
    // EXECUTE NEXT STEP
    // =========================================================

    @Override
    public StepExecutionResult executeNextStep(Integer executionId) {

        WorkflowExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new BaseException("Workflow execution not found",
                        ExceptionCodes.WORKFLOW_EXECUTION_NOT_FOUND));

        FeatureWorkflowStep step =
                stepRepository.findById(execution.getCurrentStepId())
                        .orElseThrow(() -> new BaseException("Workflow step not found",
                                ExceptionCodes.WORKFLOW_STEP_NOT_FOUND));

        return executeStep(step, execution);
    }

    // =========================================================
    // HANDLE USER ACTION
    // =========================================================

    @Override
    public StepExecutionResult handleUserAction(Integer executionId,
                                                UserActionRequest request) {

        WorkflowExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new BaseException("Workflow execution not found",
                        ExceptionCodes.WORKFLOW_EXECUTION_NOT_FOUND));

        WorkflowStepExecution stepExecution =
                stepExecutionRepository
                        .findTopByWorkflowExecution_WorkflowExecutionIdOrderByStepOrderDesc(executionId)
                        .orElseThrow(() -> new BaseException("Workflow execution step not found",
                                ExceptionCodes.EXECUTION_STEP_NOT_FOUND));

        // 1. Mark selected action
        WorkflowActionExecution action =
                actionExecutionRepository
                        .findByStepExecutionAndValue(stepExecution, request.getActionValue())
                        .orElseThrow(() -> new BaseException("Invalid workflow action",
                                ExceptionCodes.INVALID_WORKFLOW_ACTION));

        action.setSelected(true);
        actionExecutionRepository.save(action);

        // 2. Save input
        if (request.getInputJson() != null) {
            stepExecution.setInputJson(request.getInputJson());
        }

        stepExecution.setStatus(StepExecutionStatus.COMPLETED);
        stepExecutionRepository.save(stepExecution);

        // 3. Move to next step
        FeatureWorkflowStep nextStep =
                stepRepository.findNextStep(
                        execution.getWorkflowId(),
                        stepExecution.getStepOrder()
                ).orElse(null);

        if (nextStep == null) {
            execution.setStatus(WorkflowExecutionStatus.COMPLETED);
            executionRepository.save(execution);
            return buildFinalResponse(execution);
        }

        execution.setCurrentStepId(nextStep.getWorkflowStepId());
        executionRepository.save(execution);

        return executeStep(nextStep, execution);
    }

    // =========================================================
    // CORE EXECUTION ENGINE
    // =========================================================

    private StepExecutionResult executeStep(FeatureWorkflowStep step,
                                            WorkflowExecution execution) {

        // 1. Resume support (idempotency)
        // TODO: Test all possible scenarios for idempotency
        Optional<WorkflowStepExecution> existing =
                stepExecutionRepository
                        .findByWorkflowExecution_WorkflowExecutionIdAndStepId(
                                execution.getWorkflowExecutionId(),
                                step.getWorkflowStepId());

        if (existing.isPresent()) {
            return buildResponseFromExecution(existing.get());
        }

        // 2. Create new step execution
        WorkflowStepExecution stepExecution = WorkflowStepExecution.builder()
                .workflowExecution(execution)
                .stepId(step.getWorkflowStepId())
                .stepOrder(step.getStepOrder())
                .status(StepExecutionStatus.IN_PROGRESS)
                .build();

        stepExecutionRepository.save(stepExecution);

        // 3. Build context
        WorkflowExecutionContext context = buildContext(execution);
        context.put("currentStepExecution", stepExecution);

        // 4. Delegate to correct executor
        StepExecutor executor =
                stepExecutorFactory.getExecutor(step.getStepActor());

        StepExecutionResult result =
                executor.execute(step, execution, context);

        // =====================================================
        // AUTO PROGRESSION FOR SYSTEM & AI
        // =====================================================

        if (step.getStepActor() != WorkflowStepActor.USER) {

            FeatureWorkflowStep nextStep =
                    stepRepository.findNextStep(
                            execution.getWorkflowId(),
                            step.getStepOrder()
                    ).orElse(null);

            if (nextStep == null) {
                execution.setStatus(WorkflowExecutionStatus.COMPLETED);
                executionRepository.save(execution);
                return buildFinalResponse(execution);
            }

            execution.setCurrentStepId(nextStep.getWorkflowStepId());
            executionRepository.save(execution);

            // TODO: Verify: Can recursion have problems here?
            return executeStep(nextStep, execution); // recursive progression
        }

        return result;
    }

    // =========================================================
    // BUILD CONTEXT
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
    // RESUME RESPONSE
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

    // =========================================================
    // FINAL RESPONSE
    // =========================================================

    private StepExecutionResult buildFinalResponse(WorkflowExecution execution) {

        return StepExecutionResult.builder()
                .stepId(execution.getCurrentStepId())
                .stepName("Completed")
                .instruction("Workflow completed successfully")
                .actions(Collections.emptyList())
                .status("COMPLETED")
                .isTerminal(true)
                .build();
    }
}
