package com.ayushsingh.doc_helper.features.product_features.service.service_impl;

import com.ayushsingh.doc_helper.core.exception_handling.ExceptionCodes;
import com.ayushsingh.doc_helper.core.exception_handling.exceptions.BaseException;
import com.ayushsingh.doc_helper.features.product_features.entity.UsageQuota;
import com.ayushsingh.doc_helper.features.product_features.execution.FeatureUsageMetrics;
import com.ayushsingh.doc_helper.features.product_features.repository.UsageQuotaRepository;
import com.ayushsingh.doc_helper.features.product_features.service.UsageQuotaService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UsageQuotaServiceImpl implements UsageQuotaService {

    private final UsageQuotaRepository usageQuotaRepository;

    @Transactional
    @Override
    public void consume(
            Long userId,
            String featureCode,
            String metric,
            long amount) {
        int updated = usageQuotaRepository.consumeIfAvailable(
                userId, featureCode, metric, amount);

        if (updated > 0) {
            return;
        }

        var quota = usageQuotaRepository
                .findByUserIdAndFeatureCodeAndMetric(userId, featureCode, metric)
                .orElseThrow(() -> new BaseException(
                        "Quota not configured",
                        ExceptionCodes.QUOTA_NOT_FOUND));

        if (quota.getUsed() + amount > quota.getLimit()) {
            throw new BaseException("Quota exceeded", ExceptionCodes.QUOTA_EXCEEDED);
        }
    }

    @Override
    public void assertQuotaAvailable(Long userId, String featureCode, long amount) {
        var quota = usageQuotaRepository
                .findByUserIdAndFeatureCodeAndMetric(userId, featureCode, FeatureUsageMetrics.TOKENS)
                .orElseThrow(() -> new BaseException(
                        "Quota not configured",
                        ExceptionCodes.QUOTA_NOT_FOUND));

        if (quota.getUsed() + amount > quota.getLimit()) {
            throw new BaseException("Quota exceeded", ExceptionCodes.QUOTA_EXCEEDED);
        }
    }

    @Override
    public List<UsageQuota> findByUserAndFeatureCodes(Long userId, List<String> featureCodes) {
        return usageQuotaRepository.findByUserAndFeatureCodes(userId, featureCodes);
    }
}
