package com.ayushsingh.doc_helper.features.feature_workflow.dto.step_action_config;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserActionRequest {

    private Integer stepId;

    /**
     * Value selected by user (maps to ActionDTO.value)
     */
    private String actionValue;

    /**
     * Optional payload (file upload, text input, etc.)
     */
    private String inputJson;

    private Boolean reExecute;
}