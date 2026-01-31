package com.ayushsingh.doc_helper.features.product_features.service;

import com.ayushsingh.doc_helper.features.product_features.dto.FeatureResponse;

import java.util.List;

public interface FeatureCacheService {
    List<FeatureResponse> getCachedHomeFeatures(Long userId);
    
    void cacheHomeFeatures(Long userId, List<FeatureResponse> response);
    
    void evictHomeFeatures(Long userId);
}
