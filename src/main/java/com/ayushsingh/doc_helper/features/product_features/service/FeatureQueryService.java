package com.ayushsingh.doc_helper.features.product_features.service;

import com.ayushsingh.doc_helper.features.product_features.dto.FeatureResponse;
import com.ayushsingh.doc_helper.features.product_features.dto.UsageInfoListResponse;

public interface FeatureQueryService {
    FeatureResponse getProductFeatures(Long userId);

    UsageInfoListResponse getUsageInfoForUser(Long userId);
}
