package com.ayushsingh.doc_helper.features.product_features.service;

import com.ayushsingh.doc_helper.features.product_features.dto.*;

public interface AdminFeatureService {

    ProductFeatureDto createFeature(FeatureCreateRequestDto request);

    ProductFeatureDto updateFeature(
            String featureCode,
            FeatureUpdateRequestDto request
    );

    void deleteFeature(String featureCode);

    void enableFeature(String featureCode);
    
    void disableFeature(String featureCode);
    
    FeatureUIConfigDto updateUI(String featureCode, FeatureUIUpdateRequest req);
    
    FeatureActionDto updateAction(String featureCode, FeatureActionUpdateRequest req);
}
