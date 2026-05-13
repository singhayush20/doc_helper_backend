package com.ayushsingh.doc_helper.features.feature_workflow.dto_to_entity_mapper;

import com.ayushsingh.doc_helper.features.feature_workflow.dto.step_action_config.ActionDto;
import com.ayushsingh.doc_helper.features.feature_workflow.entity.WorkflowStepActionConfig;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public class ActionMapper {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static ActionDto fromConfig(WorkflowStepActionConfig config) {

        Map<String, Object> uiSchema = parseJson(config.getUiSchemaJson());

        return ActionDto.builder()
                .label((String) uiSchema.getOrDefault("label", ""))
                .value((String) uiSchema.getOrDefault("value", config.getActionType().name()))
                .uiType(config.getActionUiType())
                .metadata(uiSchema)
                .selected(false)
                .build();
    }

    private static Map<String, Object> parseJson(String json) {
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Invalid uiSchema JSON", e);
        }
    }
}