package com.ayushsingh.doc_helper.features.feature_workflow.dto.step_action_config;

import lombok.*;

import java.util.Map;

import com.ayushsingh.doc_helper.features.feature_workflow.entity.enums.StepActionUiType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActionDto {

    /**
     * Display text
     */
    private String label;

    /**
     * Internal value (used for selection)
     */
    private String value;

    /**
     * UI type (CHIP, BUTTON, LIST_ITEM, etc.)
     */
    private StepActionUiType uiType;

    /**
     * Extra metadata (confidence, icons, etc.)
     */
    private Map<String, Object> metadata;

    /**
     * Whether user selected this
     */
    private Boolean selected;
}