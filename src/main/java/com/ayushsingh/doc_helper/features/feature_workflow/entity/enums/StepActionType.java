package com.ayushsingh.doc_helper.features.feature_workflow.entity.enums;

public enum StepActionType {
    SELECT("SELECT"),
    SUBMIT("SUBMIT"),
    REGENERATE("REGENERATE"),
    SHARE("SHARE"),
    DOWNLOAD("DOWNLOAD"),
    UPLOAD("UPLOAD");

    StepActionType(String value) {
        this.value = value;
    }

    private String value;

    String getValue() {
        return value;
    }
}
