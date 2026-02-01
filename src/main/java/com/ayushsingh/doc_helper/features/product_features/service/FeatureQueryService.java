package com.ayushsingh.doc_helper.features.product_features.service;

import com.ayushsingh.doc_helper.features.product_features.dto.FeatureResponse;

public interface FeatureQueryService {
    FeatureResponse getProductFeatures(Long userId);
}
