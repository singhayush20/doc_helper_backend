package com.ayushsingh.doc_helper.features.product_features.service;

import com.ayushsingh.doc_helper.features.product_features.dto.ui.FeatureScreenResponse;
import com.ayushsingh.doc_helper.features.product_features.dto.ui_component.UIComponentDetailsDto;
import com.ayushsingh.doc_helper.features.product_features.entity.UIComponentType;
import com.fasterxml.jackson.databind.JsonNode;
public interface UIComponentService {

    UIComponentDetailsDto createUIComponent(JsonNode uiConfig,
                                            Long productFeatureId,
                                            UIComponentType uiComponentType,
                                            Integer version,
                                            Integer featureUIVersion,
                                            String screen);

    FeatureScreenResponse getUIFeatures(Long userId, String screen, UIComponentType componentType);
}
