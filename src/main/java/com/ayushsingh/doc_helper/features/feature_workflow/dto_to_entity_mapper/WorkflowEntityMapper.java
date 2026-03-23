package com.ayushsingh.doc_helper.features.feature_workflow.dto_to_entity_mapper;

import java.util.ArrayList;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import com.ayushsingh.doc_helper.core.exception_handling.ExceptionCodes;
import com.ayushsingh.doc_helper.core.exception_handling.exceptions.BaseException;
import com.ayushsingh.doc_helper.features.feature_workflow.dto.feature_workflow_step.FeatureWorkflowStepCreateDto;
import com.ayushsingh.doc_helper.features.feature_workflow.dto.feature_workflow_step.FeatureWorkflowStepDetailsDto;
import com.ayushsingh.doc_helper.features.feature_workflow.dto.step_action_config.WorkflowStepActionConfigDetailsDto;
import com.ayushsingh.doc_helper.features.feature_workflow.dto.workflow.WorkflowCreateDto;
import com.ayushsingh.doc_helper.features.feature_workflow.dto.workflow.WorkflowDetailsDto;
import com.ayushsingh.doc_helper.features.feature_workflow.entity.FeatureWorkflow;
import com.ayushsingh.doc_helper.features.feature_workflow.entity.FeatureWorkflowStep;
import com.ayushsingh.doc_helper.features.feature_workflow.entity.WorkflowStepActionConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WorkflowEntityMapper {

    private final ModelMapper modelMapper;
    private final ObjectMapper objectMapper;

    public FeatureWorkflow toWorkflowEntity(WorkflowCreateDto createDto) {
        return FeatureWorkflow.builder()
                .name(createDto.getName())
                .description(createDto.getDescription())
                .steps(new ArrayList<>())
                .build();
    }

    public FeatureWorkflowStep toStepEntity(FeatureWorkflowStepCreateDto createDto, FeatureWorkflow workflow) {
        return FeatureWorkflowStep.builder()
                .workflow(workflow)
                .name(createDto.getName())
                .stepOrder(createDto.getStepOrder())
                .stepActor(createDto.getStepActor())
                .instruction(createDto.getInstruction())
                .actions(new ArrayList<>())
                .build();
    }

    public WorkflowStepActionConfig toActionEntity(
            WorkflowStepActionConfigDetailsDto actionConfigDto,
            FeatureWorkflowStep workflowStep) {
        return WorkflowStepActionConfig.builder()
                .actionType(actionConfigDto.getActionType())
                .actionUiType(actionConfigDto.getActionUiType())
                .workflowStep(workflowStep)
                .uiSchemaJson(writeJson(actionConfigDto.getUiJson()))
                .build();
    }

    public WorkflowDetailsDto toWorkflowDetailsDto(FeatureWorkflow workflow) {
        var workflowDetails = modelMapper.map(workflow, WorkflowDetailsDto.class);
        workflowDetails.setSteps(mapStepDetails(workflow.getSteps()));
        return workflowDetails;
    }

    public FeatureWorkflowStepDetailsDto toStepDetailsDto(FeatureWorkflowStep workflowStep) {
        var stepDetails = modelMapper.map(workflowStep, FeatureWorkflowStepDetailsDto.class);
        stepDetails.setActions(mapActionDetails(workflowStep.getActions()));
        return stepDetails;
    }

    public WorkflowStepActionConfigDetailsDto toActionDetailsDto(WorkflowStepActionConfig actionConfig) {
        var actionDetails = modelMapper.map(actionConfig, WorkflowStepActionConfigDetailsDto.class);
        actionDetails.setUiJson(readJson(actionConfig.getUiSchemaJson()));
        return actionDetails;
    }

    public List<FeatureWorkflowStepDetailsDto> mapStepDetails(List<FeatureWorkflowStep> steps) {
        if (steps == null || steps.isEmpty()) {
            return List.of();
        }

        return steps.stream()
                .map(this::toStepDetailsDto)
                .toList();
    }

    public List<WorkflowStepActionConfigDetailsDto> mapActionDetails(List<WorkflowStepActionConfig> actionConfigs) {
        if (actionConfigs == null || actionConfigs.isEmpty()) {
            return List.of();
        }

        return actionConfigs.stream()
                .map(this::toActionDetailsDto)
                .toList();
    }

    private String writeJson(JsonNode jsonNode) {
        if (jsonNode == null || jsonNode.isNull()) {
            throw new BaseException("Action uiJson is required", ExceptionCodes.INVALID_WORKFLOW_STEP_UI_CONFIG);
        }

        try {
            return objectMapper.writeValueAsString(jsonNode);
        } catch (JsonProcessingException e) {
            throw new BaseException("Invalid action uiJson", ExceptionCodes.INVALID_WORKFLOW_STEP_UI_CONFIG);
        }
    }

    private JsonNode readJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }

        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            throw new BaseException("Invalid stored action uiJson", ExceptionCodes.INVALID_WORKFLOW_STEP_UI_CONFIG);
        }
    }
}
