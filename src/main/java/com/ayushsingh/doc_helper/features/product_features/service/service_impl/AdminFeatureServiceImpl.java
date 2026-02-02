package com.ayushsingh.doc_helper.features.product_features.service.service_impl;

import com.ayushsingh.doc_helper.core.exception_handling.ExceptionCodes;
import com.ayushsingh.doc_helper.core.exception_handling.exceptions.BaseException;
import com.ayushsingh.doc_helper.features.product_features.cache.FeatureInvalidationPublisher;
import com.ayushsingh.doc_helper.features.product_features.dto.FeatureCreateRequestDto;
import com.ayushsingh.doc_helper.features.product_features.dto.FeatureUpdateRequestDto;
import com.ayushsingh.doc_helper.features.product_features.dto.ProductFeatureDto;
import com.ayushsingh.doc_helper.features.product_features.dto.ui_component.UIComponentCreateRequestDto;
import com.ayushsingh.doc_helper.features.product_features.dto.ui_component.UIComponentDetailsDto;
import com.ayushsingh.doc_helper.features.product_features.entity.Feature;
import com.ayushsingh.doc_helper.features.product_features.entity.FeatureType;
import com.ayushsingh.doc_helper.features.product_features.repository.FeatureRepository;
import com.ayushsingh.doc_helper.features.product_features.service.AdminFeatureService;
import com.ayushsingh.doc_helper.features.product_features.service.UIComponentService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminFeatureServiceImpl implements AdminFeatureService {

    private final FeatureRepository featureRepository;
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

        var updatedFeature = featureRepository.save(feature);

        invalidationPublisher.publishFeatureListInvalidation();


        return modelMapper.map(updatedFeature, ProductFeatureDto.class);
    }

    @Transactional
    @Override
    public void enableFeature(String featureCode) {
        Feature feature = getFeature(featureCode);
        feature.setActive(true);
        featureRepository.save(feature);

        invalidationPublisher.publishFeatureListInvalidation();
    }

    @Transactional
    @Override
    public void disableFeature(String featureCode) {
        Feature feature = getFeature(featureCode);
        feature.setActive(false);
        featureRepository.save(feature);

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

    private Feature getFeature(String code) {
        return featureRepository.findByCodeAndActiveTrue(code)
                .orElseThrow(() ->
                        new BaseException("Feature not found", ExceptionCodes.FEATURE_NOT_FOUND));
    }
}
