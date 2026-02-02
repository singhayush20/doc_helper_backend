package com.ayushsingh.doc_helper.features.product_features.cache;

import com.ayushsingh.doc_helper.features.product_features.service.FeatureCacheService;
import com.ayushsingh.doc_helper.features.product_features.service.UIComponentCacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
@Component
@RequiredArgsConstructor
@Slf4j
public class FeatureInvalidationSubscriber
        implements MessageListener {

    private final FeatureCacheService featureCacheService;
    private final UIComponentCacheService uiCacheService;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(@NonNull Message message, byte[] pattern) {
        try {
            FeatureInvalidationEvent event =
                    objectMapper.readValue(
                            message.getBody(),
                            FeatureInvalidationEvent.class
                    );

            switch (event.getType()) {

                case FEATURE_LIST_CHANGED -> {
                    featureCacheService.bumpGlobalVersion();
                    log.info("Feature list cache version bumped");
                }

                case SUBSCRIPTION_CHANGED -> {
                    Long userId = event.getUserId();
                    if (userId != null) {
                        featureCacheService.evictProductFeatures(userId);
                        log.info(
                                "Feature cache evicted for user {}",
                                userId
                        );
                    }
                }

                case FEATURE_UI_CHANGED -> {
                    UIInvalidationPayload payload =
                            objectMapper.convertValue(
                                    event.getPayload(),
                                    UIInvalidationPayload.class
                            );

                    uiCacheService.evictUI(
                            payload.featureId(),
                            payload.screen(),
                            payload.featureUIVersion()
                    );

                    log.info(
                            "UI cache evicted for feature {}, screen {}, version {}",
                            payload.featureId(),
                            payload.screen(),
                            payload.featureUIVersion()
                    );
                }

                default -> log.warn(
                        "Unhandled invalidation type: {}",
                        event.getType()
                );
            }

        } catch (Exception e) {
            log.warn(
                    "Error occurred when processing feature invalidation event",
                    e
            );
        }
    }
}
