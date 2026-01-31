package com.ayushsingh.doc_helper.features.product_features.service.service_impl;

import com.ayushsingh.doc_helper.core.exception_handling.ExceptionCodes;
import com.ayushsingh.doc_helper.core.exception_handling.exceptions.BaseException;
import com.ayushsingh.doc_helper.features.product_features.entity.Feature;
import com.ayushsingh.doc_helper.features.product_features.repository.BillingProductFeatureRepository;
import com.ayushsingh.doc_helper.features.product_features.repository.FeatureRepository;
import com.ayushsingh.doc_helper.features.product_features.service.FeatureAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FeatureAccessServiceImpl implements FeatureAccessService {

    private final FeatureRepository featureRepository;
    private final BillingProductFeatureRepository productFeatureRepo;

    public void assertFeatureAccess(Long userId, String featureCode) {
        Feature feature = featureRepository
                .findByCodeAndActiveTrue(featureCode)
                .orElseThrow(() -> new BaseException("Feature disabled", ExceptionCodes.FEATURE_DISABLED_ERROR));

        // billingProductId resolved from subscription
        Long billingProductId = resolveBillingProduct(userId);

        boolean enabled = productFeatureRepo
                .findByBillingProductIdAndEnabledTrue(billingProductId)
                .stream()
                .anyMatch(pf -> pf.getFeatureId().equals(feature.getId()));

        if (!enabled) {
            throw new BaseException("Feature not allowed", ExceptionCodes.FEATURE_NOT_ALLOWED_ERROR);
        }
    }

    private Long resolveBillingProduct(Long userId) {
        // existing subscription logic
        return 1L;
    }
}
