package com.ayushsingh.doc_helper.features.product_features.execution;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class FeatureExecutorRegistry {

    private final Map<FeatureCodes, FeatureExecutor<?, ?>> registry;

    public FeatureExecutorRegistry(List<FeatureExecutor<?, ?>> executors) {
        Map<FeatureCodes, FeatureExecutor<?, ?>> map = new HashMap<>();
        for (FeatureExecutor<?, ?> executor : executors) {
            FeatureCodes code = executor.featureCode();
            FeatureExecutor<?, ?> existing = map.put(code, executor);
            if (existing != null) {
                throw new IllegalStateException(
                        "Duplicate feature executor for code: " + code);
            }
        }
        this.registry = Collections.unmodifiableMap(map);
    }

    @SuppressWarnings("unchecked")
    public <I, O> FeatureExecutor<I, O> get(FeatureCodes featureCode) {
        FeatureExecutor<?, ?> executor = registry.get(featureCode);
        if (executor == null) {
            throw new IllegalStateException("No executor registered for feature: " + featureCode);
        }
        return (FeatureExecutor<I, O>) executor;
    }

    public boolean hasExecutor(FeatureCodes featureCode) {
        return registry.containsKey(featureCode);
    }
}
