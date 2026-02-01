package com.ayushsingh.doc_helper.features.product_features.service;

import com.ayushsingh.doc_helper.features.product_features.dto.FeatureResponse;

import java.util.List;

public interface FeatureQueryService {
    List<FeatureResponse> getProductFeatures(Long userId);
}
