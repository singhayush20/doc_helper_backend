package com.ayushsingh.doc_helper.features.product_features.service.service_impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.ayushsingh.doc_helper.features.product_features.entity.BillingProductFeature;
import com.ayushsingh.doc_helper.features.product_features.repository.BillingProductFeatureRepository;
import com.ayushsingh.doc_helper.features.product_features.service.BillingProductFeatureService;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class BillingProductFeatureServiceImpl implements
        BillingProductFeatureService {

    private final BillingProductFeatureRepository billingProductFeatureRepository;

    @Override
    public List<BillingProductFeature> getEnabledByBillingProductId(Long billingProductId) {
        return billingProductFeatureRepository
                .findEnabledByBillingProduct(billingProductId);
    }
}
