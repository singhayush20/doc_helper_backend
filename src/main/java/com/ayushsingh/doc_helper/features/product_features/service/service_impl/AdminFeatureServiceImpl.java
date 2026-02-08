package com.ayushsingh.doc_helper.features.product_features.service.service_impl;

import com.ayushsingh.doc_helper.core.exception_handling.ExceptionCodes;
import com.ayushsingh.doc_helper.core.exception_handling.exceptions.BaseException;
import com.ayushsingh.doc_helper.features.product_features.cache.FeatureInvalidationPublisher;
import com.ayushsingh.doc_helper.features.product_features.dto.FeatureCreateRequestDto;
import com.ayushsingh.doc_helper.features.product_features.dto.FeatureUpdateRequestDto;
import com.ayushsingh.doc_helper.features.product_features.dto.ProductFeatureDto;
import com.ayushsingh.doc_helper.features.product_features.dto.feature_product.BillingProductFeatureDetailsDto;
import com.ayushsingh.doc_helper.features.product_features.dto.feature_product.BillingProductFeatureKeyRequestDto;
import com.ayushsingh.doc_helper.features.product_features.dto.feature_product.BillingProductFeatureMapRequestDto;
import com.ayushsingh.doc_helper.features.product_features.dto.ui_component.UIComponentCreateRequestDto;
import com.ayushsingh.doc_helper.features.product_features.dto.ui_component.UIComponentDetailsDto;
import com.ayushsingh.doc_helper.features.product_features.entity.BillingProductFeature;
import com.ayushsingh.doc_helper.features.product_features.entity.Feature;
import com.ayushsingh.doc_helper.features.product_features.entity.FeatureType;
import com.ayushsingh.doc_helper.features.product_features.entity.UsageMetric;
import com.ayushsingh.doc_helper.features.product_features.execution.FeatureCodes;
import com.ayushsingh.doc_helper.features.product_features.repository.FeatureRepository;
import com.ayushsingh.doc_helper.features.product_features.repository.FeatureUIConfigRepository;
import com.ayushsingh.doc_helper.features.product_features.repository.BillingProductFeatureRepository;
import com.ayushsingh.doc_helper.features.product_features.service.AdminFeatureService;
import com.ayushsingh.doc_helper.features.product_features.service.UIComponentService;
import com.ayushsingh.doc_helper.features.user_plan.repository.BillingProductRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminFeatureServiceImpl implements AdminFeatureService {

    private final FeatureRepository featureRepository;
    private final BillingProductFeatureRepository billingProductFeatureRepository;
    private final BillingProductRepository billingProductRepository;
    private final FeatureUIConfigRepository featureUIConfigRepository;
    private final FeatureInvalidationPublisher invalidationPublisher;
    private final ModelMapper modelMapper;
    private final UIComponentService uiComponentService;

    @Override
    public ProductFeatureDto createFeature(FeatureCreateRequestDto featureCreateRequestDto) {

        featureRepository.findByCode(featureCreateRequestDto.getCode())
                .ifPresent(feature -> {
                    throw new BaseException(
                            "Feature already exists",
                            ExceptionCodes.DUPLICATE_FEATURE_ERROR
                    );
                });

        var feature = new Feature();
        feature.setCode(featureCreateRequestDto.getCode());
        feature.setName(featureCreateRequestDto.getName());
        feature.setDescription(featureCreateRequestDto.getDescription());
        feature.setType(FeatureType.valueOf(featureCreateRequestDto.getType()));
        feature.setUsageMetric(requireUsageMetric(featureCreateRequestDto.getUsageMetric()));
        feature.setActive(false); // default false

        var savedFeature = featureRepository.save(feature);

        invalidationPublisher.publishFeatureListInvalidation();

        return modelMapper.map(savedFeature, ProductFeatureDto.class);
    }

    @Override
    public ProductFeatureDto updateFeature(
            String featureCode,
            FeatureUpdateRequestDto req
    ) {
        var feature = getFeature(featureCode);

        feature.setName(req.getName());
        feature.setDescription(req.getDescription());
        feature.setType(FeatureType.valueOf(req.getType()));
        if (req.getUsageMetric() != null) {
            feature.setUsageMetric(UsageMetric.valueOf(req.getUsageMetric()));
        }

        var updatedFeature = featureRepository.save(feature);

        invalidationPublisher.publishFeatureListInvalidation();


        return modelMapper.map(updatedFeature, ProductFeatureDto.class);
    }

    @Transactional
    @Override
    public void enableFeature(String featureCode) {
        int updated = featureRepository.updateActiveByCode(
                parseFeatureCode(featureCode),
                true
        );

        if (updated == 0) {
            throw new BaseException(
                    "Feature not found",
                    ExceptionCodes.FEATURE_NOT_FOUND
            );
        }

        invalidationPublisher.publishFeatureListInvalidation();
    }

    @Transactional
    @Override
    public void disableFeature(String featureCode) {
        int updated = featureRepository.updateActiveByCode(
                parseFeatureCode(featureCode),
                false
        );

        if (updated == 0) {
            throw new BaseException(
                    "Feature not found",
                    ExceptionCodes.FEATURE_NOT_FOUND
            );
        }

        invalidationPublisher.publishFeatureListInvalidation();
    }

    @Override
    public UIComponentDetailsDto createUIComponent(UIComponentCreateRequestDto dto) {
        var uiConfig = dto.getUiConfig();
        var productFeatureId = dto.getProductFeatureId();
        var isProductPresent = featureRepository.existsById(productFeatureId);

        if (!isProductPresent) {
            throw new BaseException("Product with id " + productFeatureId + " is not present", ExceptionCodes.FEATURE_NOT_FOUND);
        }

        var uiComponentDetailsDto =
                uiComponentService.createUIComponent(uiConfig,
                        productFeatureId,dto.getUiComponentType(),
                        dto.getVersion(),dto.getUiFeatureVersion(),dto.getScreen());
        invalidationPublisher.publishUIInvalidation(
                productFeatureId,
                dto.getScreen(),
                dto.getUiFeatureVersion());
        return uiComponentDetailsDto;
    }

    @Override
    public void deleteFeature(String featureCode) {
        var feature = getFeature(featureCode);

        // Soft delete (recommended)
        feature.setActive(false);
        featureRepository.save(feature);

        invalidationPublisher.publishFeatureListInvalidation();
    }

    @Transactional
    @Override
    public BillingProductFeatureDetailsDto addFeatureToBillingProduct(
            BillingProductFeatureMapRequestDto dto
    ) {
        validateProductAndFeature(dto.getProductId(), dto.getFeatureId());

        billingProductFeatureRepository
                .findByBillingProductIdAndFeatureId(
                        dto.getProductId(),
                        dto.getFeatureId()
                )
                .ifPresent(existing -> {
                    throw new BaseException(
                            "Feature already mapped to billing product",
                            ExceptionCodes.DUPLICATE_FEATURE_ERROR
                    );
                });

        Integer enabledVersion = requireEnabledVersion(dto.getEnabledVersion());
        validateUiVersion(dto.getFeatureId(), enabledVersion);

        BillingProductFeature mapping = new BillingProductFeature();
        mapping.setBillingProductId(dto.getProductId());
        mapping.setFeatureId(dto.getFeatureId());
        mapping.setEnabledVersion(enabledVersion);
        mapping.setQuotaLimit(requireQuotaLimit(dto.getQuotaLimit()));
        mapping.setPriority(dto.getPriority() != null ? dto.getPriority() : 0);
        mapping.setEnabled(false);

        BillingProductFeature saved =
                billingProductFeatureRepository.save(mapping);

        invalidationPublisher.publishFeatureListInvalidation();

        return toDetailsDto(saved);
    }

    @Transactional
    @Override
    public BillingProductFeatureDetailsDto updateProductFeatureConfig(
            BillingProductFeatureDetailsDto dto
    ) {
        BillingProductFeature mapping = resolveMapping(dto);

        if (dto.getEnabledVersion() != null &&
                !dto.getEnabledVersion().equals(mapping.getEnabledVersion())) {
            validateUiVersion(mapping.getFeatureId(), dto.getEnabledVersion());
            mapping.setEnabledVersion(dto.getEnabledVersion());
        }

        if (dto.getPriority() != null) {
            mapping.setPriority(dto.getPriority());
        }

        if (dto.getEnabled() != null) {
            mapping.setEnabled(dto.getEnabled());
        }

        if (dto.getQuotaLimit() != null) {
            mapping.setQuotaLimit(dto.getQuotaLimit());
        }

        BillingProductFeature saved =
                billingProductFeatureRepository.save(mapping);

        invalidationPublisher.publishFeatureListInvalidation();

        return toDetailsDto(saved);
    }

    @Transactional
    @Override
    public void removeFeatureFromBillingProduct(
            BillingProductFeatureKeyRequestDto dto
    ) {
        BillingProductFeature mapping = resolveMapping(dto);

        if (mapping.isEnabled()) {
            throw new BaseException(
                    "Disable the feature before removal",
                    ExceptionCodes.FEATURE_NOT_ALLOWED_ERROR
            );
        }

        billingProductFeatureRepository.delete(mapping);
        invalidationPublisher.publishFeatureListInvalidation();
    }

    @Transactional
    @Override
    public void enableFeatureForProduct(
            BillingProductFeatureKeyRequestDto dto
    ) {
        int updated = billingProductFeatureRepository.updateEnabledForProductFeature(
                requireProductId(dto),
                requireFeatureId(dto),
                true
        );

        if (updated == 0) {
            throw new BaseException(
                    "Feature mapping not found",
                    ExceptionCodes.FEATURE_NOT_FOUND
            );
        }

        invalidationPublisher.publishFeatureListInvalidation();
    }

    @Transactional
    @Override
    public void disableFeatureForProduct(
            BillingProductFeatureKeyRequestDto dto
    ) {
        int updated = billingProductFeatureRepository.updateEnabledForProductFeature(
                requireProductId(dto),
                requireFeatureId(dto),
                false
        );

        if (updated == 0) {
            throw new BaseException(
                    "Feature mapping not found",
                    ExceptionCodes.FEATURE_NOT_FOUND
            );
        }

        invalidationPublisher.publishFeatureListInvalidation();
    }

    private Feature getFeature(String code) {
        return featureRepository.findByCode(parseFeatureCode(code))
                .orElseThrow(() ->
                        new BaseException("Feature not found", ExceptionCodes.FEATURE_NOT_FOUND));
    }

    private void validateProductAndFeature(Long productId, Long featureId) {
        if (productId == null || featureId == null) {
            throw new BaseException(
                    "Product id and feature id are required",
                    ExceptionCodes.FEATURE_NOT_FOUND
            );
        }

        if (!billingProductRepository.existsById(productId)) {
            throw new BaseException(
                    "Billing product not found",
                    ExceptionCodes.PRODUCT_NOT_FOUND
            );
        }

        if (!featureRepository.existsById(featureId)) {
            throw new BaseException(
                    "Feature not found",
                    ExceptionCodes.FEATURE_NOT_FOUND
            );
        }
    }

    private Integer requireEnabledVersion(Integer enabledVersion) {
        if (enabledVersion == null) {
            throw new BaseException(
                    "Enabled version is required",
                    ExceptionCodes.INVALID_UI_CONFIG
            );
        }
        return enabledVersion;
    }

    private void validateUiVersion(Long featureId, Integer enabledVersion) {
        boolean exists =
                featureUIConfigRepository.existsByFeatureIdAndFeatureUiVersion(
                        featureId,
                        Math.toIntExact(enabledVersion)
                );
        if (!exists) {
            throw new BaseException(
                    "UI config not found for feature/version",
                    ExceptionCodes.INVALID_UI_CONFIG
            );
        }
    }

    private UsageMetric requireUsageMetric(String usageMetric) {
        if (usageMetric == null || usageMetric.isBlank()) {
            throw new BaseException(
                    "Usage metric is required",
                    ExceptionCodes.INVALID_FEATURE_CONFIG
            );
        }
        return UsageMetric.valueOf(usageMetric);
    }

    private Long requireQuotaLimit(Long quotaLimit) {
        if (quotaLimit == null) {
            throw new BaseException(
                    "Quota limit is required",
                    ExceptionCodes.INVALID_FEATURE_CONFIG
            );
        }
        return quotaLimit;
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

    private BillingProductFeatureDetailsDto toDetailsDto(
            BillingProductFeature mapping
    ) {
        return BillingProductFeatureDetailsDto.builder()
                .id(mapping.getId())
                .productId(mapping.getBillingProductId())
                .featureId(mapping.getFeatureId())
                .enabledVersion(mapping.getEnabledVersion())
                .enabled(mapping.isEnabled())
                .priority(mapping.getPriority())
                .quotaLimit(mapping.getQuotaLimit())
                .build();
    }

    private BillingProductFeature resolveMapping(
            BillingProductFeatureDetailsDto dto
    ) {
        if (dto.getId() != null) {
            return billingProductFeatureRepository.findById(dto.getId())
                    .orElseThrow(() -> new BaseException(
                            "Feature mapping not found",
                            ExceptionCodes.FEATURE_NOT_FOUND
                    ));
        }

        if (dto.getProductId() == null || dto.getFeatureId() == null) {
            throw new BaseException(
                    "Mapping id or (productId + featureId) required",
                    ExceptionCodes.FEATURE_NOT_FOUND
            );
        }

        return billingProductFeatureRepository
                .findByBillingProductIdAndFeatureId(
                        dto.getProductId(),
                        dto.getFeatureId()
                )
                .orElseThrow(() -> new BaseException(
                        "Feature mapping not found",
                        ExceptionCodes.FEATURE_NOT_FOUND
                ));
    }

    private BillingProductFeature resolveMapping(
            BillingProductFeatureKeyRequestDto dto
    ) {
        return billingProductFeatureRepository
                .findByBillingProductIdAndFeatureId(
                        requireProductId(dto),
                        requireFeatureId(dto)
                )
                .orElseThrow(() -> new BaseException(
                        "Feature mapping not found",
                        ExceptionCodes.FEATURE_NOT_FOUND
                ));
    }

    private Long requireProductId(BillingProductFeatureKeyRequestDto dto) {
        if (dto.getProductId() == null) {
            throw new BaseException(
                    "Product id is required",
                    ExceptionCodes.FEATURE_NOT_FOUND
            );
        }
        return dto.getProductId();
    }

    private Long requireFeatureId(BillingProductFeatureKeyRequestDto dto) {
        if (dto.getFeatureId() == null) {
            throw new BaseException(
                    "Feature id is required",
                    ExceptionCodes.FEATURE_NOT_FOUND
            );
        }
        return dto.getFeatureId();
    }
}
