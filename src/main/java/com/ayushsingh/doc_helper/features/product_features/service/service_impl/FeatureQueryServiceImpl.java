package com.ayushsingh.doc_helper.features.product_features.service.service_impl;

import com.ayushsingh.doc_helper.features.product_features.dto.*;
import com.ayushsingh.doc_helper.features.product_features.entity.*;
import com.ayushsingh.doc_helper.features.product_features.repository.*;
import com.ayushsingh.doc_helper.features.product_features.service.FeatureCacheService;
import com.ayushsingh.doc_helper.features.product_features.service.FeatureQueryService;
import com.ayushsingh.doc_helper.features.user_plan.service.SubscriptionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeatureQueryServiceImpl implements FeatureQueryService {

    private final BillingProductFeatureRepository billingProductFeatureRepository;
    private final FeatureRepository featureRepository;
    private final FeatureUIConfigRepository featureUIConfigRepository;
    private final FeatureActionRepository featureActionRepository;
    private final UsageQuotaRepository usageQuotaRepository;
    private final SubscriptionService subscriptionService;
    private final FeatureCacheService featureCacheService;
    private final ModelMapper modelMapper;
    private final ObjectMapper objectMapper;

    @Override
    public FeatureResponse getProductFeatures(Long userId) {

        FeatureResponse cachedFeatureResponse =
                featureCacheService.getCachedProductFeatures(userId);

        if (cachedFeatureResponse != null) {
            return cachedFeatureResponse;
        }

        Long billingProductId = resolveBillingProduct(userId);

        List<BillingProductFeature> billingProductFeatureMapping =
                billingProductFeatureRepository.findEnabledByBillingProduct(billingProductId);

        if (billingProductFeatureMapping.isEmpty()) {
            return new FeatureResponse(List.of());
        }

        List<Long> featureIds = billingProductFeatureMapping.stream()
                .map(BillingProductFeature::getFeatureId)
                .toList();

        List<Feature> features =
                featureRepository.findActiveByIds(featureIds);

        // Map featureId → Feature
        Map<Long, Feature> featureMap = features.stream()
                .collect(Collectors.toMap(Feature::getId, f -> f));

        Map<Long, FeatureUIConfig> uiMap =
                featureUIConfigRepository.findAllById(featureIds).stream()
                        .collect(Collectors.toMap(
                                FeatureUIConfig::getFeatureId, ui -> ui
                        ));

        Map<Long, FeatureAction> actionMap =
                featureActionRepository.findAll().stream()
                        .filter(a -> featureIds.contains(a.getFeatureId()))
                        .filter(FeatureAction::isEnabled)
                        .collect(Collectors.toMap(
                                FeatureAction::getFeatureId, a -> a
                        ));

        List<String> featureCodes = features.stream()
                .map(Feature::getCode)
                .toList();

        Map<String, UsageQuota> quotaMap =
                usageQuotaRepository.findByUserAndFeatureCodes(userId, featureCodes)
                        .stream()
                        .collect(Collectors.toMap(
                                UsageQuota::getFeatureCode, q -> q
                        ));

        List<ProductFeatureDto> productFeatureDtoList = billingProductFeatureMapping.stream()
                .sorted(Comparator.comparing(
                        BillingProductFeature::getPriority,
                        Comparator.nullsLast(Integer::compareTo)
                ))
                .map(billingProductFeature -> {
                    var feature = featureMap.get(billingProductFeature.getFeatureId());
                    if (feature == null) return null;

                    var ui = uiMap.get(feature.getId());
                    var action = actionMap.get(feature.getId());
                    var quota = quotaMap.get(feature.getCode());

                    return ProductFeatureDto.builder()
                            .code(feature.getCode())
                            .name(feature.getName())
                            .uiConfig(featureUiConfigToDto(ui))
                            .action(featureActionToDto(action))
                            .quota(userQuotaToDto(quota))
                            .build();
                })
                .filter(Objects::nonNull)
                .filter(productFeatureDto ->
                        productFeatureDto.getUiConfig() == null ||
                        productFeatureDto.getUiConfig().isVisible())
                .toList();
        var featureResponse = new FeatureResponse(productFeatureDtoList);
        featureCacheService.cacheProductFeatures(userId, featureResponse);

        return featureResponse;
    }

    private Long resolveBillingProduct(Long userId) {
        return subscriptionService.getBillingProductIdBySubscriptionId(userId);
    }

    private FeatureUIConfigDto featureUiConfigToDto(FeatureUIConfig featureUiConfig) {
        if (featureUiConfig == null) return null;
        return modelMapper.map(featureUiConfig,FeatureUIConfigDto.class);
    }

    private FeatureActionDto featureActionToDto(FeatureAction action) {
        if (action == null) return null;

        return FeatureActionDto.builder()
                .kind(action.getKind())
                .destination(action.getDestination())
                .payload(parseJson(action.getPayload()))
                .build();
    }

    private UsageQuotaDto userQuotaToDto(UsageQuota quota) {
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

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJson(String json) {
        if (json == null) return Map.of();
        // ObjectMapper injected in real code
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            log.error("Error occurred when parsing feature action json: {}", json, e);
        }
        return Map.of();
    }
}
