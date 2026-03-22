package com.ayushsingh.doc_helper.features.feature_workflow.service.service_impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ayushsingh.doc_helper.core.exception_handling.ExceptionCodes;
import com.ayushsingh.doc_helper.core.exception_handling.exceptions.BaseException;
import com.ayushsingh.doc_helper.features.feature_workflow.dto.step_action_config.WorkflowStepActionConfigDetailsDto;
import com.ayushsingh.doc_helper.features.feature_workflow.repository.FeatureWorkflowStepRepository;
import com.ayushsingh.doc_helper.features.feature_workflow.repository.WorkflowStepActionConfigRepository;
import com.ayushsingh.doc_helper.features.feature_workflow.service.WorkflowEntityMapper;
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
        validateCreateRequest(workflowStepId, actionConfigDetailsDto);

        var workflowStep = workflowStepRepository.findById(workflowStepId)
                .orElseThrow(() -> new BaseException("Workflow step not found", ExceptionCodes.FEATURE_NOT_FOUND));

        var actionConfig = workflowEntityMapper.toActionEntity(actionConfigDetailsDto, workflowStep);
        var savedActionConfig = actionConfigRepository.save(actionConfig);

        return workflowEntityMapper.toActionDetailsDto(savedActionConfig);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkflowStepActionConfigDetailsDto getActionConfigById(Integer stepUiId) {
        var actionConfig = actionConfigRepository.findByStepUiId(stepUiId)
                .orElseThrow(() -> new BaseException("Workflow action config not found", ExceptionCodes.FEATURE_NOT_FOUND));
        return workflowEntityMapper.toActionDetailsDto(actionConfig);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkflowStepActionConfigDetailsDto> getActionConfigsByWorkflowStepId(Integer workflowStepId) {
        if (!workflowStepRepository.existsById(workflowStepId)) {
            throw new BaseException("Workflow step not found", ExceptionCodes.FEATURE_NOT_FOUND);
        }

        return actionConfigRepository.findAllByWorkflowStep_WorkflowStepIdOrderByStepUiIdAsc(workflowStepId)
                .stream()
                .map(workflowEntityMapper::toActionDetailsDto)
                .toList();
    }

    private void validateCreateRequest(
            Integer workflowStepId,
            WorkflowStepActionConfigDetailsDto actionConfigDetailsDto) {
        if (workflowStepId == null) {
            throw new BaseException("Workflow step id is required", ExceptionCodes.FEATURE_NOT_FOUND);
        }
        if (actionConfigDetailsDto == null) {
            throw new BaseException("Workflow action config payload is required", ExceptionCodes.INVALID_FEATURE_CONFIG);
        }
        if (actionConfigDetailsDto.getActionType() == null) {
            throw new BaseException("Action type is required", ExceptionCodes.INVALID_FEATURE_CONFIG);
        }
        if (actionConfigDetailsDto.getActionUiType() == null) {
            throw new BaseException("Action ui type is required", ExceptionCodes.INVALID_FEATURE_CONFIG);
        }
        if (actionConfigDetailsDto.getUiJson() == null || actionConfigDetailsDto.getUiJson().isNull()) {
            throw new BaseException("Action uiJson is required", ExceptionCodes.INVALID_UI_CONFIG);
        }
    }
}
