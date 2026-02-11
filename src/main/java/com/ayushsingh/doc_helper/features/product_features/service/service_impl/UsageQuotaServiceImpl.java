package com.ayushsingh.doc_helper.features.product_features.service.service_impl;

import com.ayushsingh.doc_helper.core.exception_handling.ExceptionCodes;
import com.ayushsingh.doc_helper.core.exception_handling.exceptions.BaseException;
import com.ayushsingh.doc_helper.features.product_features.entity.BillingProductFeature;
import com.ayushsingh.doc_helper.features.product_features.entity.Feature;
import com.ayushsingh.doc_helper.features.product_features.entity.UsageMetric;
import com.ayushsingh.doc_helper.features.product_features.entity.UsageQuota;
import com.ayushsingh.doc_helper.features.product_features.execution.FeatureCodes;
import com.ayushsingh.doc_helper.features.product_features.repository.BillingProductFeatureRepository;
import com.ayushsingh.doc_helper.features.product_features.repository.FeatureRepository;
import com.ayushsingh.doc_helper.features.product_features.repository.UsageQuotaRepository;
import com.ayushsingh.doc_helper.features.product_features.service.UsageQuotaService;
import com.ayushsingh.doc_helper.features.user_plan.entity.AccountTier;
import com.ayushsingh.doc_helper.features.user_plan.service.BillingProductService;
import com.ayushsingh.doc_helper.features.user_plan.service.SubscriptionService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
public class UsageQuotaServiceImpl implements UsageQuotaService {

    private final UsageQuotaRepository usageQuotaRepository;
    private final FeatureRepository featureRepository;
    private final BillingProductFeatureRepository billingProductFeatureRepository;
    private final SubscriptionService subscriptionService;
    private final BillingProductService billingProductService;

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    @Override
    public void consume(
            Long userId,
            String featureCode,
            UsageMetric metric,
            long amount) {
        getOrCreateQuota(userId, featureCode, metric);
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

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    @Override
    public long getRemainingTokens(Long userId, String featureCode) {
        var quota = getOrCreateQuota(
                userId,
                featureCode,
                UsageMetric.TOKEN_COUNT
        );

        long remaining = quota.getLimit() - quota.getUsed();
        return Math.max(0, remaining);
    }

    @Override
    public List<UsageQuota> findByUserAndFeatureCodes(Long userId, List<String> featureCodes) {
        return usageQuotaRepository.findByUserAndFeatureCodes(userId, featureCodes);
    }

    @Override
    public Page<UsageQuota> findQuotasToResetPaginated(Instant now, Pageable pageable) {
        return usageQuotaRepository.findQuotasToResetPaginated(now, pageable);
    }

    @Transactional
    @Override
    public void resetQuotaForNewBillingCycle(Long quotaId) {
        UsageQuota quota = usageQuotaRepository.findById(quotaId)
                .orElseThrow(() -> new BaseException(
                        "Quota not configured",
                        ExceptionCodes.QUOTA_NOT_FOUND));

        quota.setUsed(0L);
        quota.setResetAt(oneMonthFromNow());
        usageQuotaRepository.save(quota);
    }

    private UsageQuota getOrCreateQuota(
            Long userId,
            String featureCode,
            UsageMetric metric
    ) {
        return usageQuotaRepository
                .findByUserIdAndFeatureCodeAndMetric(userId, featureCode, metric)
                .orElseGet(() -> createQuota(userId, featureCode, metric));
    }

    private UsageQuota createQuota(
            Long userId,
            String featureCode,
            UsageMetric metric
    ) {
        Feature feature = featureRepository.findByCode(parseFeatureCode(featureCode))
                .orElseThrow(() -> new BaseException(
                        "Feature not found",
                        ExceptionCodes.FEATURE_NOT_FOUND));

        if (feature.getUsageMetric() != metric) {
            throw new BaseException(
                    "Usage metric mismatch",
                    ExceptionCodes.INVALID_FEATURE_CONFIG
            );
        }

        Long billingProductId = resolveBillingProduct(userId);
        BillingProductFeature mapping = billingProductFeatureRepository
                .findByBillingProductIdAndFeatureIdAndEnabledTrue(
                        billingProductId,
                        feature.getId()
                )
                .orElseThrow(() -> new BaseException(
                        "Feature not allowed",
                        ExceptionCodes.FEATURE_NOT_ALLOWED_ERROR));

        UsageQuota quota = new UsageQuota();
        quota.setUserId(userId);
        quota.setFeatureCode(featureCode);
        quota.setMetric(metric);
        quota.setLimit(mapping.getQuotaLimit());
        quota.setUsed(0L);
        quota.setResetAt(oneMonthFromNow());

        try {
            return usageQuotaRepository.save(quota);
        } catch (DataIntegrityViolationException ex) {
            return usageQuotaRepository
                    .findByUserIdAndFeatureCodeAndMetric(userId, featureCode, metric)
                    .orElseThrow(() -> new BaseException(
                            "Quota not configured",
                            ExceptionCodes.QUOTA_NOT_FOUND));
        }
    }

    private FeatureCodes parseFeatureCode(String code) {
        try {
            return FeatureCodes.valueOf(code);
        } catch (Exception e) {
            throw new BaseException(
                    "Invalid feature code",
                    ExceptionCodes.INVALID_FEATURE_CONFIG
            );
        }
    }

    private Long resolveBillingProduct(Long userId) {
        return subscriptionService.getBillingProductIdBySubscriptionId(userId)
                .orElse(billingProductService.getProductIdByTier(AccountTier.FREE));
    }

    private Instant oneMonthFromNow() {
        return ZonedDateTime.now(ZoneId.of("Asia/Kolkata"))
                .plusMonths(1)
                .toInstant();
    }
}
