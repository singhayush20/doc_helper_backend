package com.ayushsingh.doc_helper.features.product_features.service;

import java.util.List;

import com.ayushsingh.doc_helper.features.product_features.entity.BillingProductFeature;

public interface BillingProductFeatureService {
    List<BillingProductFeature> getEnabledByBillingProductId(Long billingProductId);
}
