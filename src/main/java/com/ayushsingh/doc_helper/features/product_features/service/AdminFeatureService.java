package com.ayushsingh.doc_helper.features.product_features.service;

import com.ayushsingh.doc_helper.features.product_features.dto.*;
import com.ayushsingh.doc_helper.features.product_features.dto.feature_product.BillingProductFeatureDetailsDto;
import com.ayushsingh.doc_helper.features.product_features.dto.feature_product.BillingProductFeatureKeyRequestDto;
import com.ayushsingh.doc_helper.features.product_features.dto.feature_product.BillingProductFeatureMapRequestDto;
import com.ayushsingh.doc_helper.features.product_features.dto.ui_component.UIComponentCreateRequestDto;
import com.ayushsingh.doc_helper.features.product_features.dto.ui_component.UIComponentDetailsDto;

public interface AdminFeatureService {

    ProductFeatureDto createFeature(FeatureCreateRequestDto request);

    ProductFeatureDto updateFeature(
            String featureCode,
            FeatureUpdateRequestDto request
    );

    void deleteFeature(String featureCode);

    void enableFeature(String featureCode);
    
    void disableFeature(String featureCode);

    UIComponentDetailsDto createUIComponent(UIComponentCreateRequestDto dto);

    BillingProductFeatureDetailsDto addFeatureToBillingProduct(
            BillingProductFeatureMapRequestDto dto
    );

    BillingProductFeatureDetailsDto updateProductFeatureConfig(
            BillingProductFeatureDetailsDto dto
    );

    void removeFeatureFromBillingProduct(
            BillingProductFeatureKeyRequestDto dto
    );

    void enableFeatureForProduct(
            BillingProductFeatureKeyRequestDto dto
    );

    void disableFeatureForProduct(
            BillingProductFeatureKeyRequestDto dto
    );
}
