package com.ayushsingh.doc_helper.features.product_features.service;

public interface UsageQuotaService {
    void consume(Long userId, String featureCode, String metric, long amount);
}
