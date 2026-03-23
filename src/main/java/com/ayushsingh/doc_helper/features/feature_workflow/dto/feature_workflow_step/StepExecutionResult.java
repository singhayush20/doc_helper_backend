package com.ayushsingh.doc_helper.features.feature_workflow.dto.feature_workflow_step;

import lombok.*;

import java.util.List;
import java.util.Map;

import com.ayushsingh.doc_helper.features.feature_workflow.dto.step_action_config.ActionDto;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StepExecutionResult {

    /**
     * Step metadata
     */
    private Integer stepId;
    private String stepName;
    private Integer stepOrder;

    /**
     * Instruction to show in UI
     */
    private String instruction;

    /**
     * Final merged actions (static + dynamic)
     */
    private List<ActionDto> actions;

    /**
     * Step status (WAITING_FOR_INPUT, COMPLETED, etc.)
     */
    private String status;

    @Builder.Default
    private Boolean isTerminal = false;
}