package com.ayushsingh.doc_helper.features.feature_workflow.service.service_impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ayushsingh.doc_helper.core.exception_handling.ExceptionCodes;
import com.ayushsingh.doc_helper.core.exception_handling.exceptions.BaseException;
import com.ayushsingh.doc_helper.features.feature_workflow.dto.step_action_config.WorkflowStepActionConfigDetailsDto;
import com.ayushsingh.doc_helper.features.feature_workflow.dto_to_entity_mapper.WorkflowEntityMapper;
import com.ayushsingh.doc_helper.features.feature_workflow.repository.FeatureWorkflowStepRepository;
import com.ayushsingh.doc_helper.features.feature_workflow.repository.WorkflowStepActionConfigRepository;
import com.ayushsingh.doc_helper.features.feature_workflow.service.WorkflowStepActionConfigService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkflowStepActionConfigServiceImpl implements WorkflowStepActionConfigService {

    private final FeatureWorkflowStepRepository workflowStepRepository;
    private final WorkflowStepActionConfigRepository actionConfigRepository;
    private final WorkflowEntityMapper workflowEntityMapper;

    @Override
    @Transactional
    public WorkflowStepActionConfigDetailsDto createActionConfig(
            Integer workflowStepId,
            WorkflowStepActionConfigDetailsDto actionConfigDetailsDto) {
        var workflowStep = workflowStepRepository.findById(workflowStepId)
                .orElseThrow(() -> new BaseException("Workflow step not found", ExceptionCodes.FEATURE_NOT_FOUND));

        var actionConfig = workflowEntityMapper.toActionEntity(actionConfigDetailsDto, workflowStep);
        var savedActionConfig = actionConfigRepository.save(actionConfig);

        return workflowEntityMapper.toActionDetailsDto(savedActionConfig);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkflowStepActionConfigDetailsDto getActionConfigById(Integer stepUiId) {
        var actionConfig = actionConfigRepository.findActionConfigById(stepUiId)
                .orElseThrow(() -> new BaseException("Workflow action config not found", ExceptionCodes.WORKFLOW_ACTION_CONFIG_NOT_FOUND));
        return workflowEntityMapper.toActionDetailsDto(actionConfig);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkflowStepActionConfigDetailsDto> getActionConfigsByWorkflowStepId(Integer workflowStepId) {
        if (!workflowStepRepository.existsById(workflowStepId)) {
            throw new BaseException("Workflow step not found", ExceptionCodes.WORKFLOW_STEP_NOT_FOUND);
        }

        return actionConfigRepository.findAllActionConfigsByWorkflowStepId(workflowStepId)
                .stream()
                .map(workflowEntityMapper::toActionDetailsDto)
                .toList();
    }
}
