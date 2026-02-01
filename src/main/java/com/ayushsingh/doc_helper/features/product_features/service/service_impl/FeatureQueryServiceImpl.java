package com.ayushsingh.doc_helper.features.product_features.service.service_impl;

import com.ayushsingh.doc_helper.features.product_features.dto.*;
import com.ayushsingh.doc_helper.features.product_features.entity.*;
import com.ayushsingh.doc_helper.features.product_features.repository.*;
import com.ayushsingh.doc_helper.features.product_features.service.FeatureCacheService;
import com.ayushsingh.doc_helper.features.product_features.service.FeatureQueryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private final BillingProductFeatureRepository productFeatureRepo;
    private final FeatureRepository featureRepo;
    private final FeatureUIConfigRepository uiRepo;
    private final FeatureActionRepository actionRepo;
    private final UsageQuotaRepository quotaRepo;

    private final FeatureCacheService featureCacheService;

    @Override
    public FeatureResponse getProductFeatures(Long userId) {

        FeatureResponse cached =
                featureCacheService.getCachedProductFeatures(userId);

        if (cached != null) {
            return cached;
        }

        Long billingProductId = resolveBillingProduct(userId);

        List<BillingProductFeature> mappings =
                productFeatureRepo.findEnabledByBillingProduct(billingProductId);

        if (mappings.isEmpty()) {
            return new FeatureResponse(List.of());
        }

        List<Long> featureIds = mappings.stream()
                .map(BillingProductFeature::getFeatureId)
                .toList();

        List<Feature> features =
                featureRepo.findActiveByIds(featureIds);

        // Map featureId → Feature
        Map<Long, Feature> featureMap = features.stream()
                .collect(Collectors.toMap(Feature::getId, f -> f));

        Map<Long, FeatureUIConfig> uiMap =
                uiRepo.findAllById(featureIds).stream()
                        .collect(Collectors.toMap(
                                FeatureUIConfig::getFeatureId, ui -> ui
                        ));

        Map<Long, FeatureAction> actionMap =
                actionRepo.findAll().stream()
                        .filter(a -> featureIds.contains(a.getFeatureId()))
                        .filter(FeatureAction::isEnabled)
                        .collect(Collectors.toMap(
                                FeatureAction::getFeatureId, a -> a
                        ));

        List<String> featureCodes = features.stream()
                .map(Feature::getCode)
                .toList();

        Map<String, UsageQuota> quotaMap =
                quotaRepo.findByUserAndFeatureCodes(userId, featureCodes)
                        .stream()
                        .collect(Collectors.toMap(
                                UsageQuota::getFeatureCode, q -> q
                        ));

        List<ProductFeatureDto> productFeatureDtoList = mappings.stream()
                .sorted(Comparator.comparing(
                        BillingProductFeature::getPriority,
                        Comparator.nullsLast(Integer::compareTo)
                ))
                .map(mapping -> {
                    Feature feature = featureMap.get(mapping.getFeatureId());
                    if (feature == null) return null;

                    FeatureUIConfig ui = uiMap.get(feature.getId());
                    FeatureAction action = actionMap.get(feature.getId());
                    UsageQuota quota = quotaMap.get(feature.getCode());

                    return ProductFeatureDto.builder()
                            .code(feature.getCode())
                            .name(feature.getName())
                            .uiConfig(mapUi(ui))
                            .action(mapAction(action))
                            .quota(mapQuota(quota))
                            .build();
                })
                .filter(Objects::nonNull)
                .filter(fr -> fr.getUiConfig() == null || fr.getUiConfig().isVisible())
                .toList();
        var featureResponse = new FeatureResponse(productFeatureDtoList);
        featureCacheService.cacheProductFeatures(userId, featureResponse);

        return featureResponse;
    }

    private Long resolveBillingProduct(Long userId) {
        // Your existing subscription logic
        return 1L;
    }

    /* -------- Mapping helpers -------- */

    private FeatureUIConfigDto mapUi(FeatureUIConfig ui) {
        if (ui == null) return null;

        return FeatureUIConfigDto.builder()
                .icon(ui.getIcon())
                .backgroundColor(ui.getBackgroundColor())
                .textColor(ui.getTextColor())
                .badgeText(ui.getBadgeText())
                .sortOrder(ui.getSortOrder())
                .visible(ui.isVisible())
                .build();
    }

    private FeatureActionDto mapAction(FeatureAction action) {
        if (action == null) return null;

        return FeatureActionDto.builder()
                .kind(action.getKind())
                .destination(action.getDestination())
                .payload(parseJson(action.getPayload()))
                .build();
    }

    private UsageQuotaDto mapQuota(UsageQuota quota) {
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
            return new ObjectMapper().readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            log.error("Error occurred when parsing feature action json: {}", json, e);
        }
        return Map.of();
    }
}
