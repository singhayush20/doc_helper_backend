package com.ayushsingh.doc_helper.features.product_features.repository;

import com.ayushsingh.doc_helper.features.product_features.entity.Feature;
import com.ayushsingh.doc_helper.features.product_features.entity.FeatureUIConfig;

public interface FeatureUiConfigView {
    Feature getFeature();
    FeatureUIConfig getUiConfig();
    Integer getPriority();
}
