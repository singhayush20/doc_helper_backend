package com.ayushsingh.doc_helper.features.product_features.execution;

public interface FeatureExecutor<I, O> {
    FeatureCodes featureCode();

    O execute(I input);
}
