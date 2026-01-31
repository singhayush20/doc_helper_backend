package com.ayushsingh.doc_helper.features.product_features.service.service_impl;

import com.ayushsingh.doc_helper.features.product_features.dto.FeatureActionDto;
import com.ayushsingh.doc_helper.features.product_features.dto.FeatureResponse;
import com.ayushsingh.doc_helper.features.product_features.dto.UsageQuotaDto;
import com.ayushsingh.doc_helper.features.product_features.entity.*;
import com.ayushsingh.doc_helper.features.product_features.repository.*;
import com.ayushsingh.doc_helper.features.product_features.service.FeatureCacheService;
import com.ayushsingh.doc_helper.features.product_features.service.FeatureQueryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeatureQueryServiceImpl implements FeatureQueryService {

    private final BillingProductFeatureRepository productFeatureRepo;
    private final FeatureRepository featureRepo;
    private final FeatureUIConfigRepository uiRepo;
    private final FeatureActionRepository actionRepo;
    private final UsageQuotaRepository quotaRepo;

    private final FeatureCacheService featureCacheService;

    public List<FeatureResponse> getHomeFeatures(Long userId) {

        // 1️⃣ Cache-first
        List<FeatureResponse> cached =
                featureCacheService.getCachedHomeFeatures(userId);

        if (cached != null) {
            return cached;
        }

        // 2️⃣ Resolve billing product
        Long billingProductId = resolveBillingProduct(userId);

        // 3️⃣ Enabled feature mappings
        List<BillingProductFeature> mappings =
                productFeatureRepo.findEnabledByBillingProduct(billingProductId);

        if (mappings.isEmpty()) {
            return List.of();
        }

        // 4️⃣ Fetch features
        List<Long> featureIds = mappings.stream()
                .map(BillingProductFeature::getFeatureId)
                .toList();

        List<Feature> features =
                featureRepo.findActiveByIds(featureIds);

        // Map featureId → Feature
        Map<Long, Feature> featureMap = features.stream()
                .collect(Collectors.toMap(Feature::getId, f -> f));

        // 5️⃣ Fetch UI configs
        Map<Long, FeatureUIConfig> uiMap =
                uiRepo.findAllById(featureIds).stream()
                        .collect(Collectors.toMap(
                                FeatureUIConfig::getFeatureId, ui -> ui
                        ));

        // 6️⃣ Fetch actions
        Map<Long, FeatureAction> actionMap =
                actionRepo.findAll().stream()
                        .filter(a -> featureIds.contains(a.getFeatureId()))
                        .filter(FeatureAction::isEnabled)
                        .collect(Collectors.toMap(
                                FeatureAction::getFeatureId, a -> a
                        ));

        // 7️⃣ Fetch quotas
        List<String> featureCodes = features.stream()
                .map(Feature::getCode)
                .toList();

        Map<String, UsageQuota> quotaMap =
                quotaRepo.findByUserAndFeatureCodes(userId, featureCodes)
                        .stream()
                        .collect(Collectors.toMap(
                                UsageQuota::getFeatureCode, q -> q
                        ));

        // 8️⃣ Build response
        List<FeatureResponse> response = mappings.stream()
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

                    return FeatureResponse.builder()
                            .code(feature.getCode())
                            .name(feature.getName())
                            .ui(mapUi(ui))
                            .action(mapAction(action))
                            .quota(mapQuota(quota))
                            .build();
                })
                .filter(Objects::nonNull)
                .filter(fr -> fr.getUi() == null || fr.getUi().isVisible())
                .toList();

        // 9️⃣ Cache result
        featureCacheService.cacheHomeFeatures(userId, response);

        return response;
    }

    private Long resolveBillingProduct(Long userId) {
        // Your existing subscription logic
        return 1L;
    }

    /* -------- Mapping helpers -------- */

    private FeatureUI mapUi(FeatureUIConfig ui) {
        if (ui == null) return null;

        return FeatureUI.builder()
                .icon(ui.getIcon())
                .backgroundColor(ui.getBackgroundColor())
                .textColor(ui.getTextColor())
                .badgeText(ui.getBadgeText())
                .sortOrder(ui.getSortOrder())
                .visible(ui.isVisible())
                .showInPremiumGrid(ui.isShowInPremiumGrid())
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

    private Map<String, Object> parseJson(String json) {
        if (json == null) return Map.of();
        // ObjectMapper injected in real code
        return new ObjectMapper().readValue(json, Map.class);
    }
}
