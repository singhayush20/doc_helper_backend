package com.ayushsingh.doc_helper.features.product_features.service;

import com.ayushsingh.doc_helper.features.product_features.dto.FeatureActionUpdateRequest;
import com.ayushsingh.doc_helper.features.product_features.dto.FeatureUIUpdateRequest;

public interface AdminFeatureService {
    void enableFeature(String featureCode);
    
    void disableFeature(String featureCode);
    
    void updateUI(String featureCode, FeatureUIUpdateRequest req);
    
    void updateAction(String featureCode, FeatureActionUpdateRequest req);
}
