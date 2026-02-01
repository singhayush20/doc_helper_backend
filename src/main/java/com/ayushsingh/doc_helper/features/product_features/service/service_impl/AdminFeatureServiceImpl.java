package com.ayushsingh.doc_helper.features.product_features.service.service_impl;

import com.ayushsingh.doc_helper.core.exception_handling.ExceptionCodes;
import com.ayushsingh.doc_helper.core.exception_handling.exceptions.BaseException;
import com.ayushsingh.doc_helper.features.product_features.cache.FeatureInvalidationPublisher;
import com.ayushsingh.doc_helper.features.product_features.dto.*;
import com.ayushsingh.doc_helper.features.product_features.entity.Feature;
import com.ayushsingh.doc_helper.features.product_features.entity.FeatureAction;
import com.ayushsingh.doc_helper.features.product_features.entity.FeatureType;
import com.ayushsingh.doc_helper.features.product_features.entity.FeatureUIConfig;
import com.ayushsingh.doc_helper.features.product_features.repository.FeatureActionRepository;
import com.ayushsingh.doc_helper.features.product_features.repository.FeatureRepository;
import com.ayushsingh.doc_helper.features.product_features.repository.FeatureUIConfigRepository;
import com.ayushsingh.doc_helper.features.product_features.service.AdminFeatureService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminFeatureServiceImpl implements AdminFeatureService {

    private final FeatureRepository featureRepository;
    private final FeatureUIConfigRepository featureUIConfigRepository;
    private final FeatureActionRepository featureActionRepository;
    private final FeatureInvalidationPublisher invalidationPublisher;
    private final ModelMapper modelMapper;

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

        invalidationPublisher.publishGlobalInvalidation();

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

        invalidationPublisher.publishGlobalInvalidation();

        return modelMapper.map(updatedFeature, ProductFeatureDto.class);
    }

    @Transactional
    @Override
    public void enableFeature(String featureCode) {
        Feature feature = getFeature(featureCode);
        feature.setActive(true);
        featureRepository.save(feature);

        invalidationPublisher.publishGlobalInvalidation();
    }

    @Transactional
    @Override
    public void disableFeature(String featureCode) {
        Feature feature = getFeature(featureCode);
        feature.setActive(false);
        featureRepository.save(feature);

        invalidationPublisher.publishGlobalInvalidation();
    }

    @Transactional
    @Override
    public FeatureUIConfigDto updateUI(
            String featureCode,
            FeatureUIUpdateRequest featureUIUpdateRequest
    ) {
        var feature = getFeature(featureCode);

        var featureUiConfig = featureUIConfigRepository.findById(feature.getId())
                .orElse(new FeatureUIConfig());

        featureUiConfig.setFeatureId(feature.getId());
        featureUiConfig.setIcon(featureUIUpdateRequest.getIcon());
        featureUiConfig.setBackgroundColor(featureUIUpdateRequest.getBackgroundColor());
        featureUiConfig.setTextColor(featureUIUpdateRequest.getTextColor());
        featureUiConfig.setBadgeText(featureUIUpdateRequest.getBadgeText());
        featureUiConfig.setVisible(featureUIUpdateRequest.isVisible());

        var uiConfig = featureUIConfigRepository.save(featureUiConfig);
        invalidationPublisher.publishGlobalInvalidation();

        return modelMapper.map(uiConfig, FeatureUIConfigDto.class);
    }

    @Transactional
    @Override
    public FeatureActionDto updateAction(
            String featureCode,
            FeatureActionUpdateRequest req
    ) {
        var feature = getFeature(featureCode);

        var action = featureActionRepository
                .findByFeatureIdAndEnabledTrue(feature.getId())
                .orElse(new FeatureAction());

        action.setFeatureId(feature.getId());
        action.setKind(req.getKind());
        action.setDestination(req.getDestination());
        action.setPayload(req.getPayload());
        action.setEnabled(true);

        var updatedAction = featureActionRepository.save(action);
        invalidationPublisher.publishGlobalInvalidation();

        return modelMapper.map(updatedAction, FeatureActionDto.class);
    }

    @Override
    public void deleteFeature(String featureCode) {
        var feature = getFeature(featureCode);

        // Soft delete (recommended)
        feature.setActive(false);
        featureRepository.save(feature);

        // clean configs
        featureUIConfigRepository.deleteById(feature.getId());
        featureActionRepository.deleteByFeatureId(feature.getId());

        invalidationPublisher.publishGlobalInvalidation();
    }

    private Feature getFeature(String code) {
        return featureRepository.findByCodeAndActiveTrue(code)
                .orElseThrow(() ->
                        new BaseException("Feature not found", ExceptionCodes.FEATURE_NOT_FOUND));
    }
}
