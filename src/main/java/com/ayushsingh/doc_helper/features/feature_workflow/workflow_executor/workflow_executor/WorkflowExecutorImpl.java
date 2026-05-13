package com.ayushsingh.doc_helper.features.feature_workflow.workflow_executor.workflow_executor;

import com.ayushsingh.doc_helper.core.exception_handling.ExceptionCodes;
import com.ayushsingh.doc_helper.core.exception_handling.exceptions.BaseException;
import com.ayushsingh.doc_helper.features.feature_workflow.dto.feature_workflow_step.StepExecutionResult;
import com.ayushsingh.doc_helper.features.feature_workflow.dto.step_action_config.UserActionRequest;
import com.ayushsingh.doc_helper.features.feature_workflow.entity.WorkflowExecution;
import com.ayushsingh.doc_helper.features.feature_workflow.lock.DistributedLockService;
import com.ayushsingh.doc_helper.features.feature_workflow.workflow_executor.WorkflowExecutor;
import com.ayushsingh.doc_helper.features.feature_workflow.workflow_executor.exceptions.DuplicateWorkflowStepExecutionException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class WorkflowExecutorImpl implements WorkflowExecutor {

    private static final long WORKFLOW_LOCK_TIMEOUT_MILLIS = 5000L;
    private static final String LOCK_KEY_PREFIX = "workflow_execution:";

    private final DistributedLockService distributedLockService;
    private final WorkflowExecutionTransactionalService workflowExecutionTransactionalService;

    @Override
    public StepExecutionResult startWorkflow(Integer workflowId, Long userId) {
        WorkflowExecution execution =
                workflowExecutionTransactionalService.createWorkflowExecution(workflowId, userId);

        return executeWithWorkflowLock(
                execution.getWorkflowExecutionId(),
                () -> workflowExecutionTransactionalService.executeCurrentStep(
                        execution.getWorkflowExecutionId()));
    }

    @Override
    public StepExecutionResult executeNextStep(Integer executionId) {
        return executeWithWorkflowLock(
                executionId,
                () -> workflowExecutionTransactionalService.executeCurrentStep(executionId));
    }

    @Override
    public StepExecutionResult handleUserAction(Integer executionId,
                                                UserActionRequest request) {
        return executeWithWorkflowLock(
                executionId,
                () -> workflowExecutionTransactionalService.handleUserAction(executionId, request));
    }

    private StepExecutionResult executeWithWorkflowLock(Integer executionId,
                                                        Supplier<StepExecutionResult> executionSupplier) {
        String lockKey = LOCK_KEY_PREFIX.concat(executionId.toString());
        boolean acquired = distributedLockService.acquireLock(lockKey, WORKFLOW_LOCK_TIMEOUT_MILLIS);

        if (!acquired) {
            throw new BaseException("Workflow execution is already in progress, retry later",
                    ExceptionCodes.WORKFLOW_EXECUTION_IN_PROGRESS);
        }

        try {
            return executionSupplier.get();
        } catch (DuplicateWorkflowStepExecutionException ex) {
            throw new BaseException("Duplicate workflow step execution detected",
                    ExceptionCodes.DUPLICATE_WORKFLOW_STEP_EXECUTION);
        } finally {
            distributedLockService.releaseLock(lockKey);
        }
    }
}
