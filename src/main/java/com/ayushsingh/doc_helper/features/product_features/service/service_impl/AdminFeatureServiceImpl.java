package com.ayushsingh.doc_helper.features.product_features.service.service_impl;

import com.ayushsingh.doc_helper.core.exception_handling.ExceptionCodes;
import com.ayushsingh.doc_helper.core.exception_handling.exceptions.BaseException;
import com.ayushsingh.doc_helper.features.product_features.cache.FeatureInvalidationPublisher;
import com.ayushsingh.doc_helper.features.product_features.dto.FeatureActionUpdateRequest;
import com.ayushsingh.doc_helper.features.product_features.dto.FeatureUIUpdateRequest;
import com.ayushsingh.doc_helper.features.product_features.entity.Feature;
import com.ayushsingh.doc_helper.features.product_features.entity.FeatureAction;
import com.ayushsingh.doc_helper.features.product_features.entity.FeatureUIConfig;
import com.ayushsingh.doc_helper.features.product_features.repository.FeatureActionRepository;
import com.ayushsingh.doc_helper.features.product_features.repository.FeatureRepository;
import com.ayushsingh.doc_helper.features.product_features.repository.FeatureUIConfigRepository;
import com.ayushsingh.doc_helper.features.product_features.service.AdminFeatureService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminFeatureServiceImpl implements AdminFeatureService {

    private final FeatureRepository featureRepo;
    private final FeatureUIConfigRepository uiRepo;
    private final FeatureActionRepository actionRepo;
    private final FeatureInvalidationPublisher invalidationPublisher;

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
    public void updateUI(
            String featureCode,
            FeatureUIUpdateRequest req
    ) {
        Feature feature = getFeature(featureCode);

        FeatureUIConfig ui = uiRepo.findById(feature.getId())
                .orElse(new FeatureUIConfig());

        ui.setFeatureId(feature.getId());
        ui.setIcon(req.getIcon());
        ui.setBackgroundColor(req.getBackgroundColor());
        ui.setTextColor(req.getTextColor());
        ui.setBadgeText(req.getBadgeText());
        ui.setVisible(req.isVisible());
        ui.setShowInPremiumGrid(req.isShowInPremiumGrid());

        uiRepo.save(ui);

        invalidationPublisher.publishGlobalInvalidation();
    }

    @Transactional
    @Override
    public void updateAction(
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

        actionRepo.save(action);

        invalidationPublisher.publishGlobalInvalidation();
    }

    private Feature getFeature(String code) {
        return featureRepo.findByCodeAndActiveTrue(code)
                .orElseThrow(() ->
                        new BaseException("Feature not found", ExceptionCodes.FEATURE_NOT_FOUND));
    }
}
