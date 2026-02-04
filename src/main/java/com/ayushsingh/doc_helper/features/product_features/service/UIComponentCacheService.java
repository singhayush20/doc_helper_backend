package com.ayushsingh.doc_helper.features.product_features.service;

import com.fasterxml.jackson.databind.JsonNode;

public interface UIComponentCacheService {

    JsonNode getCachedUI(
            Long featureId,
            String screen,
            Integer featureUIVersion
    );

    void cacheUI(
            Long featureId,
            String screen,
            Integer featureUIVersion,
            JsonNode uiJson
    );

    void evictUI(
            Long featureId,
            String screen,
            Integer featureUIVersion
    );
}
