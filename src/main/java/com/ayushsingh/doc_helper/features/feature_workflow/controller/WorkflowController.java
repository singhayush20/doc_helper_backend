package com.ayushsingh.doc_helper.features.feature_workflow.controller;

import com.ayushsingh.doc_helper.core.security.UserContext;
import com.ayushsingh.doc_helper.features.feature_workflow.dto.feature_workflow_step.StepExecutionResult;
import com.ayushsingh.doc_helper.features.feature_workflow.dto.step_action_config.UserActionRequest;
import com.ayushsingh.doc_helper.features.feature_workflow.workflow_executor.WorkflowExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/workflows")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowExecutor workflowExecutor;

    // =========================================================
    // START WORKFLOW
    // =========================================================

    @PostMapping("/{workflowId}/start")
    public ResponseEntity<StepExecutionResult> startWorkflow(
            @PathVariable Integer workflowId) {
        var currentUser = UserContext.getCurrentUser().getUser();
        StepExecutionResult result =
                workflowExecutor.startWorkflow(workflowId, currentUser.getId());

        return ResponseEntity.ok(result);
    }

    // =========================================================
    // EXECUTE / RESUME NEXT STEP
    // =========================================================

    @GetMapping("/executions/{executionId}/next")
    public ResponseEntity<StepExecutionResult> executeNextStep(
            @PathVariable Integer executionId) {

        StepExecutionResult result =
                workflowExecutor.executeNextStep(executionId);

        return ResponseEntity.ok(result);
    }

    // =========================================================
    // HANDLE USER ACTION
    // =========================================================

    @PostMapping("/executions/{executionId}/action")
    public ResponseEntity<StepExecutionResult> handleUserAction(
            @PathVariable Integer executionId,
            @RequestBody UserActionRequest request) {

        StepExecutionResult result =
                workflowExecutor.handleUserAction(executionId, request);

        return ResponseEntity.ok(result);
    }
}