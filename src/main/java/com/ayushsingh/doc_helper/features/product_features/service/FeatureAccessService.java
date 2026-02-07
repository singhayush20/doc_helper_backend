package com.ayushsingh.doc_helper.features.product_features.service;

import com.ayushsingh.doc_helper.features.product_features.execution.FeatureCodes;

public interface FeatureAccessService {
    void assertFeatureAccess(Long userId, FeatureCodes featureCode);
}
