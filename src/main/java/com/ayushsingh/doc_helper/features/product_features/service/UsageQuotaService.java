package com.ayushsingh.doc_helper.features.product_features.service;

import java.time.Instant;
import java.util.List;

import com.ayushsingh.doc_helper.features.product_features.entity.UsageMetric;
import com.ayushsingh.doc_helper.features.product_features.entity.UsageQuota;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UsageQuotaService {
    void consume(Long userId, String featureCode, UsageMetric metric, long amount);

    long getRemainingTokens(Long userId, String featureCode);

    List<UsageQuota> findByUserAndFeatureCodes(Long userId, List<String> featureCodes);

    Page<UsageQuota> findQuotasToResetPaginated(Instant now, Pageable pageable);

    void resetQuotaForNewBillingCycle(Long quotaId);
}
