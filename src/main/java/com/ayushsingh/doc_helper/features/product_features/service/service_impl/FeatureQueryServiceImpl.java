package com.ayushsingh.doc_helper.features.product_features.service.service_impl;

import com.ayushsingh.doc_helper.features.product_features.dto.FeatureResponse;
import com.ayushsingh.doc_helper.features.product_features.dto.ProductFeatureDto;
import com.ayushsingh.doc_helper.features.product_features.dto.UsageQuotaDto;
import com.ayushsingh.doc_helper.features.product_features.entity.BillingProductFeature;
import com.ayushsingh.doc_helper.features.product_features.entity.Feature;
import com.ayushsingh.doc_helper.features.product_features.entity.UsageQuota;
import com.ayushsingh.doc_helper.features.product_features.repository.BillingProductFeatureRepository;
import com.ayushsingh.doc_helper.features.product_features.repository.FeatureRepository;
import com.ayushsingh.doc_helper.features.product_features.repository.UsageQuotaRepository;
import com.ayushsingh.doc_helper.features.product_features.service.FeatureCacheService;
import com.ayushsingh.doc_helper.features.product_features.service.FeatureQueryService;
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

    private final BillingProductFeatureRepository billingProductFeatureRepository;
    private final FeatureRepository featureRepository;
    private final UsageQuotaRepository usageQuotaRepository;
    private final SubscriptionService subscriptionService;
    private final FeatureCacheService featureCacheService;

    @Override
    public FeatureResponse getProductFeatures(Long userId) {

        FeatureResponse cached =
                featureCacheService.getCachedProductFeatures(userId);
        if (cached != null) {
            return cached;
        }

        Long billingProductId =
                subscriptionService.getBillingProductIdBySubscriptionId(userId);

        List<BillingProductFeature> mappings =
                billingProductFeatureRepository
                        .findEnabledByBillingProduct(billingProductId);

        if (mappings.isEmpty()) {
            return new FeatureResponse(List.of());
        }

        List<Long> featureIds =
                mappings.stream()
                        .map(BillingProductFeature::getFeatureId)
                        .toList();

        Map<Long, Feature> featureMap =
                featureRepository.findActiveByIds(featureIds)
                        .stream()
                        .collect(Collectors.toMap(
                                Feature::getId, f -> f
                        ));

        Map<String, UsageQuota> quotaMap =
                usageQuotaRepository
                        .findByUserAndFeatureCodes(
                                userId,
                                featureMap.values()
                                        .stream()
                                        .map(Feature::getCode)
                                        .toList()
                        )
                        .stream()
                        .collect(Collectors.toMap(
                                UsageQuota::getFeatureCode, q -> q
                        ));

        List<ProductFeatureDto> result =
                mappings.stream()
                        .sorted(Comparator.comparing(
                                BillingProductFeature::getPriority,
                                Comparator.nullsLast(Integer::compareTo)
                        ))
                        .map(mapping -> {
                            Feature feature =
                                    featureMap.get(mapping.getFeatureId());
                            if (feature == null) return null;

                            return ProductFeatureDto.builder()
                                    .featureId(feature.getId())
                                    .code(feature.getCode())
                                    .name(feature.getName())
                                    .quota(toQuotaDto(
                                            quotaMap.get(feature.getCode())))
                                    .build();
                        })
                        .filter(Objects::nonNull)
                        .toList();

        FeatureResponse response =
                new FeatureResponse(result);

        featureCacheService.cacheProductFeatures(userId, response);
        return response;
    }

    private UsageQuotaDto toQuotaDto(UsageQuota quota) {
        if (quota == null) return null;

        return UsageQuotaDto.builder()
                .metric(quota.getMetric())
                .used(quota.getUsed())
                .limit(quota.getLimit())
                .resetAtEpochMillis(
                        quota.getResetAt() != null
                                ? quota.getResetAt().toEpochMilli()
                                : null
                )
                .build();
    }
}
