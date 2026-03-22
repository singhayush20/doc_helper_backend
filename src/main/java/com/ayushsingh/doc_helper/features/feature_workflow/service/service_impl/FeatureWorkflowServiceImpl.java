package com.ayushsingh.doc_helper.features.feature_workflow.service.service_impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ayushsingh.doc_helper.core.exception_handling.ExceptionCodes;
import com.ayushsingh.doc_helper.core.exception_handling.exceptions.BaseException;
import com.ayushsingh.doc_helper.features.feature_workflow.dto.workflow.WorkflowCreateDto;
import com.ayushsingh.doc_helper.features.feature_workflow.dto.workflow.WorkflowDetailsDto;
import com.ayushsingh.doc_helper.features.feature_workflow.entity.FeatureWorkflow;
import com.ayushsingh.doc_helper.features.feature_workflow.repository.FeatureWorkflowRepository;
import com.ayushsingh.doc_helper.features.feature_workflow.service.FeatureWorkflowService;
import com.ayushsingh.doc_helper.features.feature_workflow.service.WorkflowEntityMapper;
import com.ayushsingh.doc_helper.features.product_features.service.AdminFeatureService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FeatureWorkflowServiceImpl implements FeatureWorkflowService {

    private final FeatureWorkflowRepository workflowRepository;
    private final WorkflowEntityMapper workflowEntityMapper;
    private final AdminFeatureService adminFeatureService;

    @Override
    @Transactional
    public WorkflowDetailsDto createWorkflow(WorkflowCreateDto workflowCreateDto) {
        validateCreateRequest(workflowCreateDto);

        if (workflowRepository.existsByName(workflowCreateDto.getName())) {
            throw new BaseException("Workflow already exists", ExceptionCodes.DUPLICATE_FEATURE_ERROR);
        }

        FeatureWorkflow workflow = workflowEntityMapper.toWorkflowEntity(workflowCreateDto);
        var createStepDtos = workflowCreateDto.getSteps();

        if (createStepDtos != null && !createStepDtos.isEmpty()) {
            var steps = createStepDtos.stream()
                    .map(stepDto -> {
                        var workflowStep = workflowEntityMapper.toStepEntity(stepDto, workflow);
                        var actions = stepDto.getActions() == null
                                ? List.<com.ayushsingh.doc_helper.features.feature_workflow.entity.WorkflowStepActionConfig>of()
                                : stepDto.getActions().stream()
                                        .map(actionDto -> workflowEntityMapper.toActionEntity(actionDto, workflowStep))
                                        .toList();
                        workflowStep.setActions(new ArrayList<>(actions));
                        return workflowStep;
                    })
                    .toList();
            workflow.setSteps(new ArrayList<>(steps));
        }

        FeatureWorkflow savedWorkflow = workflowRepository.save(workflow);
        adminFeatureService.assignWorkflowToFeature(workflowCreateDto.getFeatureId(), savedWorkflow);

        return workflowEntityMapper.toWorkflowDetailsDto(savedWorkflow);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkflowDetailsDto getWorkflowById(Integer workflowId) {
        var workflow = workflowRepository.findByWorkflowId(workflowId)
                .orElseThrow(() -> new BaseException("Workflow not found", ExceptionCodes.FEATURE_NOT_FOUND));
        return workflowEntityMapper.toWorkflowDetailsDto(workflow);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkflowDetailsDto> getAllWorkflows() {
        return workflowRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(workflowEntityMapper::toWorkflowDetailsDto)
                .toList();
    }

    private void validateCreateRequest(WorkflowCreateDto workflowCreateDto) {
        if (workflowCreateDto == null) {
            throw new BaseException("Workflow create payload is required", ExceptionCodes.INVALID_FEATURE_CONFIG);
        }
        if (workflowCreateDto.getName() == null || workflowCreateDto.getName().isBlank()) {
            throw new BaseException("Workflow name is required", ExceptionCodes.INVALID_FEATURE_CONFIG);
        }
        if (workflowCreateDto.getDescription() == null || workflowCreateDto.getDescription().isBlank()) {
            throw new BaseException("Workflow description is required", ExceptionCodes.INVALID_FEATURE_CONFIG);
        }
        if (workflowCreateDto.getFeatureId() == null) {
            throw new BaseException("Feature id is required", ExceptionCodes.FEATURE_NOT_FOUND);
        }
    }
}
