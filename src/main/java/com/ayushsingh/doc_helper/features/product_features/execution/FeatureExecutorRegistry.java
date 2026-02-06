package com.ayushsingh.doc_helper.features.product_features.execution;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class FeatureExecutorRegistry {

    private final Map<String, FeatureExecutor<?, ?>> registry;

    public FeatureExecutorRegistry(List<FeatureExecutor<?, ?>> executors) {
        Map<String, FeatureExecutor<?, ?>> map = new HashMap<>();
        for (FeatureExecutor<?, ?> executor : executors) {
            String code = normalize(executor.featureCode());
            FeatureExecutor<?, ?> existing = map.put(code, executor);
            if (existing != null) {
                throw new IllegalStateException(
                        "Duplicate feature executor for code: " + code);
            }
        }
        this.registry = Collections.unmodifiableMap(map);
    }

    @SuppressWarnings("unchecked")
    public <I, O> FeatureExecutor<I, O> get(String featureCode) {
        FeatureExecutor<?, ?> executor = registry.get(normalize(featureCode));
        if (executor == null) {
            throw new IllegalStateException("No executor registered for feature: " + featureCode);
        }
        return (FeatureExecutor<I, O>) executor;
    }

    public boolean hasExecutor(String featureCode) {
        return registry.containsKey(normalize(featureCode));
    }

    private String normalize(String featureCode) {
        if (featureCode == null) {
            throw new IllegalArgumentException("Feature code cannot be null");
        }
        return featureCode.trim().toUpperCase();
    }
}
