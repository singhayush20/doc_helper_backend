package com.ayushsingh.doc_helper.features.feature_workflow.entity.converters;

import com.ayushsingh.doc_helper.features.feature_workflow.entity.WorkflowStepConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class WorkflowStepConfigConverter
        implements AttributeConverter<WorkflowStepConfig, String> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(WorkflowStepConfig attribute) {
        if (attribute == null)
            return null;

        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing stepConfig", e);
        }
    }

    @Override
    public WorkflowStepConfig convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank())
            return null;

        try {
            return objectMapper.readValue(dbData, WorkflowStepConfig.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error deserializing stepConfig", e);
        }
    }
}