package com.ayushsingh.doc_helper.features.product_features.service.service_impl;

import com.ayushsingh.doc_helper.features.product_features.dto.FeatureResponse;
import com.ayushsingh.doc_helper.features.product_features.dto.ProductFeatureDto;
import com.ayushsingh.doc_helper.features.product_features.dto.UsageQuotaDto;
import com.ayushsingh.doc_helper.features.product_features.entity.BillingProductFeature;
import com.ayushsingh.doc_helper.features.product_features.entity.Feature;
import com.ayushsingh.doc_helper.features.product_features.entity.UsageQuota;
import com.ayushsingh.doc_helper.features.product_features.repository.FeatureRepository;
import com.ayushsingh.doc_helper.features.product_features.service.BillingProductFeatureService;
import com.ayushsingh.doc_helper.features.product_features.service.FeatureCacheService;
import com.ayushsingh.doc_helper.features.product_features.service.FeatureQueryService;
import com.ayushsingh.doc_helper.features.product_features.service.UsageQuotaService;
import com.ayushsingh.doc_helper.features.user_plan.entity.AccountTier;
import com.ayushsingh.doc_helper.features.user_plan.service.BillingProductService;
import com.ayushsingh.doc_helper.features.user_plan.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeatureQueryServiceImpl implements FeatureQueryService {

        private final FeatureRepository featureRepository;
        private final SubscriptionService subscriptionService;
        private final FeatureCacheService featureCacheService;
        private final BillingProductFeatureService billingProductFeatureService;
        private final UsageQuotaService usageQuotaService;
        private final BillingProductService billingProductService;

        @Override
        public FeatureResponse getProductFeatures(Long userId) {

                FeatureResponse cachedFeatureResponse = featureCacheService.getCachedProductFeatures(userId);
                if (cachedFeatureResponse != null) {
                        return cachedFeatureResponse;
                }

                List<BillingProductFeature> billingProductFeatures =
                                fetchEnabledMappings(userId);

                if (billingProductFeatures.isEmpty()) {
                        return new FeatureResponse(List.of());
                }

                Map<Long, Feature> featureMap =
                                fetchActiveFeatures(billingProductFeatures);

                Map<String, UsageQuota> quotaMap =
                                fetchUsageQuotas(userId, featureMap);

                List<ProductFeatureDto> result =
                                composeFeatures(billingProductFeatures, featureMap, quotaMap);

                FeatureResponse response = new FeatureResponse(result);
                featureCacheService.cacheProductFeatures(userId, response);
                return response;
        }

        private List<BillingProductFeature> fetchEnabledMappings(Long userId) {
                Long billingProductId = subscriptionService.getBillingProductIdBySubscriptionId(userId).orElse(
                                billingProductService.getProductIdByTier(AccountTier.FREE));
                                
                return billingProductFeatureService.getEnabledByBillingProductId(billingProductId);
        }

        private Map<Long, Feature> fetchActiveFeatures(
                        List<BillingProductFeature> billingProductFeatures
        ) {
                List<Long> featureIds = billingProductFeatures.stream()
                                .map(BillingProductFeature::getFeatureId)
                                .toList();

                return featureRepository.findActiveByIds(featureIds)
                                .stream()
                                .collect(Collectors.toMap(
                                                Feature::getId, f -> f));
        }

        private Map<String, UsageQuota> fetchUsageQuotas(
                        Long userId,
                        Map<Long, Feature> featureMap
        ) {
                List<String> featureCodes = featureMap.values()
                                .stream()
                                .map(feature -> feature.getCode().name())
                                .toList();

                return usageQuotaService
                                .findByUserAndFeatureCodes(userId, featureCodes)
                                .stream()
                                .collect(Collectors.toMap(
                                                UsageQuota::getFeatureCode, q -> q));
        }

        private List<ProductFeatureDto> composeFeatures(
                        List<BillingProductFeature> billingProductFeatures,
                        Map<Long, Feature> featureMap,
                        Map<String, UsageQuota> quotaMap
        ) {
                return billingProductFeatures.stream()
                                .sorted(Comparator.comparing(
                                                BillingProductFeature::getPriority,
                                                Comparator.nullsLast(Integer::compareTo)))
                                .map(mapping -> {
                                        Feature feature = featureMap.get(mapping.getFeatureId());
                                        if (feature == null)
                                                return null;

                                        return ProductFeatureDto.builder()
                                                        .featureId(feature.getId())
                                                        .code(feature.getCode())
                                                        .name(feature.getName())
                                                        .usageMetric(feature.getUsageMetric())
                                                        .quota(toQuotaDto(
                                                                        quotaMap.get(feature.getCode().name())))
                                                        .build();
                                })
                                .filter(Objects::nonNull)
                                .toList();
        }

        private UsageQuotaDto toQuotaDto(UsageQuota quota) {
                if (quota == null)
                        return null;

                return UsageQuotaDto.builder()
                                .metric(quota.getMetric().name())
                                .used(quota.getUsed())
                                .limit(quota.getLimit())
                                .resetAtEpochMillis(
                                                quota.getResetAt() != null
                                                                ? quota.getResetAt().toEpochMilli()
                                                                : null)
                                .build();
        }
}
