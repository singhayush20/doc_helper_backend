package com.ayushsingh.doc_helper.features.feature_workflow.dto.workflow;

import lombok.*;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowExecutionContext {

    private Integer executionId;

    /**
     * Step outputs / shared data
     */
    @Builder.Default
    private Map<String, Object> data = new HashMap<>();

    public <T> T get(String key) {
        return (T) data.get(key);
    }

    public void put(String key, Object value) {
        data.put(key, value);
    }
}
