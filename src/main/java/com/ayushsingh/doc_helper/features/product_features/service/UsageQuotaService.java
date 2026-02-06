package com.ayushsingh.doc_helper.features.product_features.service;

import java.util.List;

import com.ayushsingh.doc_helper.features.product_features.entity.UsageQuota;

public interface UsageQuotaService {
    void consume(Long userId, String featureCode, String metric, long amount);

    void assertQuotaAvailable(Long userId, String featureCode, long amount);

    List<UsageQuota> findByUserAndFeatureCodes(Long userId, List<String> featureCodes);
}
