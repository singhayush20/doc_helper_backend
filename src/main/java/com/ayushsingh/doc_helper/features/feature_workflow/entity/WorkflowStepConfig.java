package com.ayushsingh.doc_helper.features.feature_workflow.entity;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowStepConfig {

    /**
     * Executor key → maps to handler
     * Example: TOPIC_EXTRACTOR, POST_GENERATOR
     */
    private String executor;

    /**
     * Versioning support (VERY IMPORTANT for re-runs)
     */
    private String version;

    /**
     * Dynamic params for executor
     */
    private Map<String, Object> params;

    /**
     * Where output will be stored in context
     */
    private String outputKey;
}