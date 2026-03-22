package com.ayushsingh.doc_helper.features.feature_workflow.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ayushsingh.doc_helper.features.feature_workflow.dto.feature_workflow_step.FeatureWorkflowStepCreateDto;
import com.ayushsingh.doc_helper.features.feature_workflow.dto.feature_workflow_step.FeatureWorkflowStepDetailsDto;
import com.ayushsingh.doc_helper.features.feature_workflow.dto.step_action_config.WorkflowStepActionConfigDetailsDto;
import com.ayushsingh.doc_helper.features.feature_workflow.dto.workflow.WorkflowCreateDto;
import com.ayushsingh.doc_helper.features.feature_workflow.dto.workflow.WorkflowDetailsDto;
import com.ayushsingh.doc_helper.features.feature_workflow.service.FeatureWorkflowService;
import com.ayushsingh.doc_helper.features.feature_workflow.service.FeatureWorkflowStepService;
import com.ayushsingh.doc_helper.features.feature_workflow.service.WorkflowStepActionConfigService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/workflows")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class FeatureWorkflowController {

    private final FeatureWorkflowService featureWorkflowService;
    private final FeatureWorkflowStepService featureWorkflowStepService;
    private final WorkflowStepActionConfigService workflowStepActionConfigService;

    @PostMapping
    public ResponseEntity<WorkflowDetailsDto> createWorkflow(@Valid @RequestBody WorkflowCreateDto workflowCreateDto) {
        var createdWorkflow = featureWorkflowService.createWorkflow(workflowCreateDto);
        return new ResponseEntity<>(createdWorkflow, HttpStatus.CREATED);
    }

    @GetMapping("/{workflowId}")
    public ResponseEntity<WorkflowDetailsDto> getWorkflowById(@PathVariable Integer workflowId) {
        return ResponseEntity.ok(featureWorkflowService.getWorkflowById(workflowId));
    }

    @GetMapping
    public ResponseEntity<List<WorkflowDetailsDto>> getAllWorkflows() {
        return ResponseEntity.ok(featureWorkflowService.getAllWorkflows());
    }

    @PostMapping("/{workflowId}/steps")
    public ResponseEntity<FeatureWorkflowStepDetailsDto> createWorkflowStep(
            @PathVariable Integer workflowId,
            @Valid @RequestBody FeatureWorkflowStepCreateDto stepCreateDto) {
        var createdStep = featureWorkflowStepService.createWorkflowStep(workflowId, stepCreateDto);
        return new ResponseEntity<>(createdStep, HttpStatus.CREATED);
    }

    @GetMapping("/{workflowId}/steps")
    public ResponseEntity<List<FeatureWorkflowStepDetailsDto>> getWorkflowSteps(@PathVariable Integer workflowId) {
        return ResponseEntity.ok(featureWorkflowStepService.getWorkflowStepsByWorkflowId(workflowId));
    }

    @GetMapping("/steps/{workflowStepId}")
    public ResponseEntity<FeatureWorkflowStepDetailsDto> getWorkflowStepById(@PathVariable Integer workflowStepId) {
        return ResponseEntity.ok(featureWorkflowStepService.getWorkflowStepById(workflowStepId));
    }

    @PostMapping("/steps/{workflowStepId}/actions")
    public ResponseEntity<WorkflowStepActionConfigDetailsDto> createActionConfig(
            @PathVariable Integer workflowStepId,
            @Valid @RequestBody WorkflowStepActionConfigDetailsDto actionConfigDetailsDto) {
        var createdAction = workflowStepActionConfigService.createActionConfig(workflowStepId, actionConfigDetailsDto);
        return new ResponseEntity<>(createdAction, HttpStatus.CREATED);
    }

    @GetMapping("/steps/{workflowStepId}/actions")
    public ResponseEntity<List<WorkflowStepActionConfigDetailsDto>> getActionConfigsByWorkflowStepId(
            @PathVariable Integer workflowStepId) {
        return ResponseEntity.ok(workflowStepActionConfigService.getActionConfigsByWorkflowStepId(workflowStepId));
    }

    @GetMapping("/actions/{stepUiId}")
    public ResponseEntity<WorkflowStepActionConfigDetailsDto> getActionConfigById(@PathVariable Integer stepUiId) {
        return ResponseEntity.ok(workflowStepActionConfigService.getActionConfigById(stepUiId));
    }
}
