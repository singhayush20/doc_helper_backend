package com.ayushsingh.doc_helper.features.product_features.service.service_impl;

import com.ayushsingh.doc_helper.core.exception_handling.ExceptionCodes;
import com.ayushsingh.doc_helper.core.exception_handling.exceptions.BaseException;
import com.ayushsingh.doc_helper.features.product_features.dto.ProductFeatureDto;
import com.ayushsingh.doc_helper.features.product_features.dto.ui.FeatureScreenResponse;
import com.ayushsingh.doc_helper.features.product_features.dto.ui.FeatureWithUiDto;
import com.ayushsingh.doc_helper.features.product_features.dto.ui_component.UIComponentDetailsDto;
import com.ayushsingh.doc_helper.features.product_features.entity.Feature;
import com.ayushsingh.doc_helper.features.product_features.entity.FeatureUIConfig;
import com.ayushsingh.doc_helper.features.product_features.entity.UIComponentType;
import com.ayushsingh.doc_helper.features.product_features.repository.FeatureUIConfigRepository;
import com.ayushsingh.doc_helper.features.product_features.repository.projections.FeatureUiConfigView;
import com.ayushsingh.doc_helper.features.product_features.service.UIComponentCacheService;
import com.ayushsingh.doc_helper.features.product_features.service.UIComponentService;
import com.ayushsingh.doc_helper.features.ui_components.models.UIComponent;
import com.ayushsingh.doc_helper.features.ui_components.registry.UIComponentRegistry;
import com.ayushsingh.doc_helper.features.user_plan.entity.AccountTier;
import com.ayushsingh.doc_helper.features.user_plan.service.BillingProductService;
import com.ayushsingh.doc_helper.features.user_plan.service.SubscriptionService;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class UIComponentServiceImpl implements UIComponentService {

    private final FeatureUIConfigRepository featureUIConfigRepository;
    private final UIComponentRegistry uiComponentRegistry;
    private final ObjectMapper objectMapper;
    private final UIComponentCacheService uiComponentCacheService;
    private final SubscriptionService subscriptionService;
    private final BillingProductService billingProductService;

    @Override
    public UIComponentDetailsDto createUIComponent(
            JsonNode uiConfig,
            Long productFeatureId,
            UIComponentType uiComponentType,
            Integer version,
            Integer featureUIVersion,
            String screen
    ) {
        // Resolve concrete Java type BEFORE parsing
        Class<? extends UIComponent> targetClass =
                uiComponentRegistry.resolve(uiComponentType, version);

        //  Parse JSON to concrete UI record - This IS the validation step
        try {
            validateConfig(uiConfig, targetClass);
        } catch (Exception e) {
            throw new BaseException(
                    "Invalid UI configuration for " +
                            uiComponentType + " v" + version,
                    ExceptionCodes.INVALID_UI_CONFIG
            );
        }

        var existingUiFeatureVersion =
                featureUIConfigRepository.existsByFeatureIdAndFeatureUiVersion(productFeatureId, featureUIVersion);

        if (existingUiFeatureVersion) {
            throw new BaseException("Feature already has a UI config with " +
                    "version " + featureUIVersion,
                    ExceptionCodes.DUPLICATE_UI_CONFIG_FOUND);
        }

        // Persist JSON as-is (validated snapshot)
        FeatureUIConfig entity = new FeatureUIConfig();
        entity.setFeatureId(productFeatureId);
        entity.setComponentType(uiComponentType);
        entity.setFeatureUiVersion(featureUIVersion);
        entity.setUiJson(uiConfig.toString());
        entity.setScreen(screen);
        entity.setActive(true);

        FeatureUIConfig saved =
                featureUIConfigRepository.save(entity);

        // Return minimal metadata DTO
        JsonNode uiConfigNode;
        try {
            uiConfigNode = objectMapper.readTree(saved.getUiJson());
        } catch (Exception e) {
            throw new BaseException(
                    "Failed to read stored UI config",
                    ExceptionCodes.INVALID_UI_CONFIG
            );
        }

        return UIComponentDetailsDto.
                builder()
                .id(saved.getId())
                .active(saved.isActive())
                .uiComponentType(saved.getComponentType())
                .screen(saved.getScreen())
                .uiConfig(uiConfigNode)
                .build();
    }

    public Map<Long, JsonNode> getAllUIVersionsForFeatureAndScreen(
            List<Long> featureIds,
            String screen
    ) {
        List<FeatureUIConfig> configs =
                featureUIConfigRepository
                        .findByFeatureIdInAndScreenAndActiveTrue(
                                featureIds, screen);

        return configs.stream()
                .collect(Collectors.toMap(
                        FeatureUIConfig::getFeatureId,
                        this::resolveAndValidateUI
                ));
    }


    @Override
    public FeatureScreenResponse getUIFeatures(
            Long userId,
            String screen,
            UIComponentType componentType
    ) {
        // TODO: Optimize this by caching the response- also handle cache eviction/updation in case ui configs are updated
        Long billingProductId =
                subscriptionService.getBillingProductIdBySubscriptionId(userId);

        if (billingProductId == null) {
            // if the subscription was not found, then find out the product id
            // for free tier accounts
            billingProductId =
                    billingProductService.getProductIdByTier(AccountTier.FREE);
        }

        List<FeatureUiConfigView> configs =
                featureUIConfigRepository.findEnabledUiConfigsForProduct(
                        billingProductId, screen, componentType);

        if (configs.isEmpty()) {
            return new FeatureScreenResponse(List.of());
        }

        List<FeatureWithUiDto> result = configs.stream()
                .sorted(Comparator.comparing(
                                FeatureUiConfigView::getPriority,
                                Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(view -> view.getFeature().getId()))
                .map(view -> {
                    Feature feature = view.getFeature();
                    return FeatureWithUiDto.builder()
                            .feature(ProductFeatureDto.builder()
                                    .featureId(feature.getId())
                                    .code(feature.getCode())
                                    .name(feature.getName())
                                    .usageMetric(feature.getUsageMetric())
                                    .build())
                            .ui(validateJsonNode(view.getUiConfig()))
                            .build();
                })
                .toList();

        return new FeatureScreenResponse(result);
    }

    private JsonNode validateJsonNode(FeatureUIConfig config) {
        try {
            // parse raw JSON
            JsonNode jsonNode =
                    objectMapper.readTree(config.getUiJson());

            validateJsonNodeDeserialization(config, jsonNode);

            return jsonNode;

        } catch (Exception e) {
            throw new BaseException(
                    "Error reading json tree " +
                            config.getFeatureId() +
                            " (" + config.getComponentType() +
                            " v" + config.getFeatureUiVersion() + ")",
                    ExceptionCodes.INVALID_UI_CONFIG
            );
        }
    }

    /**
     * 1. Fetch from cache
     * 2. Deserialize using registry (VALIDATION)
     * 3. Cache JsonNode
     */
    private JsonNode resolveAndValidateUI(FeatureUIConfig config) {

        JsonNode cached = uiComponentCacheService.getCachedUI(
                config.getFeatureId(),
                config.getScreen(),
                config.getFeatureUiVersion()
        );

        if (cached != null) {
            return cached;
        }

        try {
            // parse raw JSON
            JsonNode jsonNode =
                    objectMapper.readTree(config.getUiJson());

            validateJsonNodeDeserialization(config, jsonNode);
            // cache validated JSON
            uiComponentCacheService.cacheUI(
                    config.getFeatureId(),
                    config.getScreen(),
                    config.getFeatureUiVersion(),
                    jsonNode
            );

            return jsonNode;

        } catch (Exception e) {
            throw new BaseException(
                    "Error reading json tree " +
                            config.getFeatureId() +
                            " (" + config.getComponentType() +
                            " v" + config.getFeatureUiVersion() + ")",
                    ExceptionCodes.INVALID_UI_CONFIG
            );
        }
    }

    private void validateJsonNodeDeserialization(FeatureUIConfig config,
                                                 JsonNode jsonNode) {
        // resolve concrete UI type
        Class<? extends UIComponent> targetClass =
                uiComponentRegistry.resolve(
                        config.getComponentType(),
                        config.getFeatureUiVersion()
                );

        // validate by deserialization
        try {
            validateConfig(jsonNode, targetClass);

        } catch (Exception e) {
            throw new BaseException(
                    "Invalid UI config for feature " +
                            config.getFeatureId() +
                            " (" + config.getComponentType() +
                            " v" + config.getFeatureUiVersion() + ")",
                    ExceptionCodes.INVALID_UI_CONFIG
            );
        }
    }

    private <T> T validateConfig(JsonNode jsonNode, Class<T> targetClass) throws Exception {
        ObjectReader reader = objectMapper.readerFor(targetClass)
                .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return reader.readValue(jsonNode);
    }
}

