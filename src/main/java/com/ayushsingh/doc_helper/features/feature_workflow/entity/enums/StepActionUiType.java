package com.ayushsingh.doc_helper.features.feature_workflow.entity.enums;

public enum StepActionUiType {
    CHIP("CHIP"),
    TILE("TILE"),
    GRID("GRID"),
    BUTTON("BUTTON");

    StepActionUiType(String value) {
        this.value = value;
    }

    private String value;

    public String getValue() {
        return value;
    }
}
