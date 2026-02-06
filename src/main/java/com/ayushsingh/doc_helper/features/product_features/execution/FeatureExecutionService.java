package com.ayushsingh.doc_helper.features.product_features.execution;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FeatureExecutionService {

    private final FeatureExecutorRegistry registry;

    public <I, O> O execute(FeatureCodes featureCode, I input) {
        FeatureExecutor<I, O> executor = registry.get(featureCode);
        return executor.execute(input);
    }
}
