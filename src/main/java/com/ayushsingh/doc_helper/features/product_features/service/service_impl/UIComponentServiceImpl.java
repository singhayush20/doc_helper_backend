package com.ayushsingh.doc_helper.features.product_features.service.service_impl;

import com.ayushsingh.doc_helper.core.exception_handling.ExceptionCodes;
import com.ayushsingh.doc_helper.core.exception_handling.exceptions.BaseException;
import com.ayushsingh.doc_helper.features.product_features.dto.ui_component.UIComponentDetailsDto;
import com.ayushsingh.doc_helper.features.product_features.entity.FeatureUIConfig;
import com.ayushsingh.doc_helper.features.product_features.entity.UIComponentType;
import com.ayushsingh.doc_helper.features.product_features.repository.FeatureUIConfigRepository;
import com.ayushsingh.doc_helper.features.product_features.service.UIComponentCacheService;
import com.ayushsingh.doc_helper.features.product_features.service.UIComponentService;
import com.ayushsingh.doc_helper.features.ui_components.models.UIComponent;
import com.ayushsingh.doc_helper.features.ui_components.registry.UIComponentRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

    @Override
    public UIComponentDetailsDto createUIComponent(
            JsonNode uiConfig,
            Long productFeatureId,
            UIComponentType uiComponentType,
            Integer version,
            Integer featureUIVersion,
            String screen
    ) {
        /*
         * 2. Resolve concrete Java type BEFORE parsing
         */
        Class<? extends UIComponent> targetClass =
                uiComponentRegistry.resolve(uiComponentType, version);

        /*
         * 3. Parse JSON → concrete UI record
         *    This IS the validation step
         */
        try {
            objectMapper.treeToValue(uiConfig, targetClass);
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

        /*
         * 4. Persist JSON as-is (validated snapshot)
         */
        FeatureUIConfig entity = new FeatureUIConfig();
        entity.setFeatureId(productFeatureId);
        entity.setComponentType(uiComponentType);
        entity.setFeatureUiVersion(featureUIVersion);
        entity.setUiJson(uiConfig.toString());
        entity.setScreen(screen);
        entity.setActive(true);

        FeatureUIConfig saved =
                featureUIConfigRepository.save(entity);

        /*
         * 5. Return minimal metadata DTO
         */

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

    public Map<Long, JsonNode> getUIForFeatures(
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
            // Step 1: parse raw JSON
            JsonNode jsonNode =
                    objectMapper.readTree(config.getUiJson());

            // Step 2: resolve concrete UI type
            Class<? extends UIComponent> targetClass =
                    uiComponentRegistry.resolve(
                            config.getComponentType(),
                            config.getFeatureUiVersion()
                    );

            // Step 3: validate by deserialization
            objectMapper.treeToValue(jsonNode, targetClass);

            // Step 4: cache validated JSON
            uiComponentCacheService.cacheUI(
                    config.getFeatureId(),
                    config.getScreen(),
                    config.getFeatureUiVersion(),
                    jsonNode
            );

            return jsonNode;

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Invalid UI config for feature " +
                            config.getFeatureId() +
                            " (" + config.getComponentType() +
                            " v" + config.getFeatureUiVersion() + ")",
                    e
            );
        }
    }
}

