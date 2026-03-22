package com.ayushsingh.doc_helper.features.feature_workflow.service.service_impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.ayushsingh.doc_helper.core.exception_handling.ExceptionCodes;
import com.ayushsingh.doc_helper.core.exception_handling.exceptions.BaseException;
import com.ayushsingh.doc_helper.features.feature_workflow.dto.workflow.WorkflowCreateDto;
import com.ayushsingh.doc_helper.features.feature_workflow.dto.workflow.WorkflowDetailsDto;
import com.ayushsingh.doc_helper.features.feature_workflow.dto_to_entity_mapper.WorkflowEntityMapper;
import com.ayushsingh.doc_helper.features.feature_workflow.entity.FeatureWorkflow;
import com.ayushsingh.doc_helper.features.feature_workflow.entity.WorkflowStepActionConfig;
import com.ayushsingh.doc_helper.features.feature_workflow.repository.FeatureWorkflowRepository;
import com.ayushsingh.doc_helper.features.feature_workflow.service.FeatureWorkflowService;
import com.ayushsingh.doc_helper.features.product_features.service.AdminFeatureService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FeatureWorkflowServiceImpl implements FeatureWorkflowService {

    private final FeatureWorkflowRepository workflowRepository;
    private final WorkflowEntityMapper workflowEntityMapper;
    private final AdminFeatureService adminFeatureService;

    @Override
    @Transactional
    public WorkflowDetailsDto createWorkflow(@Valid WorkflowCreateDto workflowCreateDto) {
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
                                ? List.<WorkflowStepActionConfig>of()
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
                .orElseThrow(() -> new BaseException("Workflow not found", ExceptionCodes.WORKFLOW_NOT_FOUND));
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
}
