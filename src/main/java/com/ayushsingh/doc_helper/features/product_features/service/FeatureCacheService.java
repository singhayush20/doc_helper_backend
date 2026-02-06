package com.ayushsingh.doc_helper.features.product_features.service;

import com.ayushsingh.doc_helper.features.product_features.dto.FeatureResponse;

public interface FeatureCacheService {
    FeatureResponse getCachedProductFeatures(Long userId);
    
    void cacheProductFeatures(Long userId, FeatureResponse response);

    void evictProductFeatures(Long userId);

    void bumpGlobalVersion();
}
