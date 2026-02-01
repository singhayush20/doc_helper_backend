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

    private final FeatureRepository featureRepo;
    private final FeatureUIConfigRepository uiRepo;
    private final FeatureActionRepository actionRepo;
    private final FeatureInvalidationPublisher invalidationPublisher;
    private final ModelMapper modelMapper;

    @Override
    public ProductFeatureDto createFeature(FeatureCreateRequestDto req) {

        featureRepo.findByCode(req.getCode())
                .ifPresent(f -> {
                    throw new BaseException(
                            "Feature already exists",
                            ExceptionCodes.DUPLICATE_FEATURE_ERROR
                    );
                });

        Feature feature = new Feature();
        feature.setCode(req.getCode());
        feature.setName(req.getName());
        feature.setDescription(req.getDescription());
        feature.setType(FeatureType.valueOf(req.getType()));
        feature.setActive(false); // default disabled

        featureRepo.save(feature);

        invalidationPublisher.publishGlobalInvalidation();

        return modelMapper.map(feature, ProductFeatureDto.class);
    }

    @Override
    public ProductFeatureDto updateFeature(
            String featureCode,
            FeatureUpdateRequestDto req
    ) {
        Feature feature = getFeature(featureCode);

        feature.setName(req.getName());
        feature.setDescription(req.getDescription());
        feature.setType(FeatureType.valueOf(req.getType()));

        featureRepo.save(feature);

        invalidationPublisher.publishGlobalInvalidation();

        return modelMapper.map(feature, ProductFeatureDto.class);
    }

    @Transactional
    @Override
    public void enableFeature(String featureCode) {
        Feature feature = getFeature(featureCode);
        feature.setActive(true);
        featureRepo.save(feature);

        invalidationPublisher.publishGlobalInvalidation();
    }

    @Transactional
    @Override
    public void disableFeature(String featureCode) {
        Feature feature = getFeature(featureCode);
        feature.setActive(false);
        featureRepo.save(feature);

        invalidationPublisher.publishGlobalInvalidation();
    }

    @Transactional
    @Override
    public FeatureUIConfigDto updateUI(
            String featureCode,
            FeatureUIUpdateRequest req
    ) {
        Feature feature = getFeature(featureCode);

        var ui = uiRepo.findById(feature.getId())
                .orElse(new FeatureUIConfig());

        ui.setFeatureId(feature.getId());
        ui.setIcon(req.getIcon());
        ui.setBackgroundColor(req.getBackgroundColor());
        ui.setTextColor(req.getTextColor());
        ui.setBadgeText(req.getBadgeText());
        ui.setVisible(req.isVisible());

        var uiConfig = uiRepo.save(ui);
        invalidationPublisher.publishGlobalInvalidation();

        return modelMapper.map(uiConfig,FeatureUIConfigDto.class);
    }

    @Transactional
    @Override
    public FeatureActionDto updateAction(
            String featureCode,
            FeatureActionUpdateRequest req
    ) {
        Feature feature = getFeature(featureCode);

        FeatureAction action = actionRepo
                .findByFeatureIdAndEnabledTrue(feature.getId())
                .orElse(new FeatureAction());

        action.setFeatureId(feature.getId());
        action.setKind(req.getKind());
        action.setDestination(req.getDestination());
        action.setPayload(req.getPayload());
        action.setEnabled(true);

        var updatedAction = actionRepo.save(action);
        invalidationPublisher.publishGlobalInvalidation();

        return modelMapper.map(updatedAction,FeatureActionDto.class);
    }

    @Override
    public void deleteFeature(String featureCode) {
        Feature feature = getFeature(featureCode);

        // Soft delete (recommended)
        feature.setActive(false);
        featureRepo.save(feature);

        // Optionally clean configs
        uiRepo.deleteById(feature.getId());
        actionRepo.deleteByFeatureId(feature.getId());

        invalidationPublisher.publishGlobalInvalidation();
    }

    private Feature getFeature(String code) {
        return featureRepo.findByCodeAndActiveTrue(code)
                .orElseThrow(() ->
                        new BaseException("Feature not found", ExceptionCodes.FEATURE_NOT_FOUND));
    }
}
