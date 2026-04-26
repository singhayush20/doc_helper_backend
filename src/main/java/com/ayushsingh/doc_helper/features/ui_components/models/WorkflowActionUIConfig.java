package com.ayushsingh.doc_helper.features.ui_components.models;

import java.util.List;

import com.google.auto.value.AutoValue.Builder;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowActionUIConfig {
    
    private WorkflowActionButton workflowActionButton;
    private WorkflowActionChip workflowActionChip;
    private WorkflowActionGridOption workflowActionGridOption;
    private WorkflowActionTile workflowActionTile;
    private List<TextInfo> actionsContentConfig;
}
