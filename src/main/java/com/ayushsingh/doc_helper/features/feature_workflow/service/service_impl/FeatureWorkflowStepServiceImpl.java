package com.ayushsingh.doc_helper.features.feature_workflow.service.service_impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.ayushsingh.doc_helper.core.exception_handling.ExceptionCodes;
import com.ayushsingh.doc_helper.core.exception_handling.exceptions.BaseException;
import com.ayushsingh.doc_helper.features.feature_workflow.dto.feature_workflow_step.FeatureWorkflowStepCreateDto;
import com.ayushsingh.doc_helper.features.feature_workflow.dto.feature_workflow_step.FeatureWorkflowStepDetailsDto;
import com.ayushsingh.doc_helper.features.feature_workflow.dto_to_entity_mapper.WorkflowEntityMapper;
import com.ayushsingh.doc_helper.features.feature_workflow.repository.FeatureWorkflowRepository;
import com.ayushsingh.doc_helper.features.feature_workflow.repository.FeatureWorkflowStepRepository;
import com.ayushsingh.doc_helper.features.feature_workflow.service.FeatureWorkflowStepService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FeatureWorkflowStepServiceImpl implements FeatureWorkflowStepService {

    private final FeatureWorkflowRepository workflowRepository;
    private final FeatureWorkflowStepRepository workflowStepRepository;
    private final WorkflowEntityMapper workflowEntityMapper;

    @Override
    @Transactional
    public FeatureWorkflowStepDetailsDto createWorkflowStep(
            Integer workflowId,
            FeatureWorkflowStepCreateDto stepCreateDto) {
        var workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new BaseException("Workflow not found", ExceptionCodes.WORKFLOW_NOT_FOUND));

        var step = workflowEntityMapper.toStepEntity(stepCreateDto, workflow);
        var actionDtos = stepCreateDto.getActions();
        if (actionDtos != null && !actionDtos.isEmpty()) {
            var actions = actionDtos.stream()
                    .map(actionDto -> workflowEntityMapper.toActionEntity(actionDto, step))
                    .toList();
            step.setActions(new ArrayList<>(actions));
        }

        var savedStep = workflowStepRepository.save(step);
        return workflowEntityMapper.toStepDetailsDto(savedStep);
    }

    @Override
    @Transactional(readOnly = true)
    public FeatureWorkflowStepDetailsDto getWorkflowStepById(Integer workflowStepId) {
        var step = workflowStepRepository.findStepById(workflowStepId)
                .orElseThrow(() -> new BaseException("Workflow step not found", ExceptionCodes.WORKFLOW_STEP_NOT_FOUND));
        return workflowEntityMapper.toStepDetailsDto(step);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeatureWorkflowStepDetailsDto> getWorkflowStepsByWorkflowId(Integer workflowId) {
        if (!workflowRepository.existsById(workflowId)) {
            throw new BaseException("Workflow not found", ExceptionCodes.WORKFLOW_STEP_NOT_FOUND);
        }

        return workflowStepRepository.findAllStepsByWorkflowId(workflowId)
                .stream()
                .map(workflowEntityMapper::toStepDetailsDto)
                .toList();
    }
}
