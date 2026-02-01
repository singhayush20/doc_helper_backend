package com.ayushsingh.doc_helper.features.product_features.cache;

import com.ayushsingh.doc_helper.features.product_features.service.FeatureCacheService;
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

    private final FeatureCacheService cacheService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void onMessage(@NonNull Message message, byte[] pattern) {
        try {
            FeatureInvalidationEvent event =
                    objectMapper.readValue(
                            message.getBody(),
                            FeatureInvalidationEvent.class
                    );

            if (event.getUserId() != null) {
                cacheService.evictProductFeatures(event.getUserId());
            } else {
                // GLOBAL invalidation
                cacheService.bumpGlobalVersion();
                log.info("Global feature cache version bumped");
            }

        } catch (Exception e) {
            log.warn("Error occurred when evicting feature ",e);
        }
    }
}
