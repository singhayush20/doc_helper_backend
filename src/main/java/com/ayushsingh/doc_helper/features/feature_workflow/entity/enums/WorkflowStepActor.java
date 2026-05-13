package com.ayushsingh.doc_helper.features.feature_workflow.entity.enums;

public enum WorkflowStepActor {
    USER("USER"),
    SYSTEM("SYSTEM"),
    AI("AI");

    WorkflowStepActor(String value) {
        this.value = value;
    }

    private String value;

    String getValue() {
        return value;
    }
}
