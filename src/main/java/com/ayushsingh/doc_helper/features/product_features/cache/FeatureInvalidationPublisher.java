package com.ayushsingh.doc_helper.features.product_features.cache;

import com.ayushsingh.doc_helper.core.pub_sub.RedisChannels;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FeatureInvalidationPublisher {

    private final RedisTemplate<String, Object> redisTemplate;

    public void publishGlobalInvalidation() {
        publish(new FeatureInvalidationEvent(
                InvalidationType.FEATURE_CONFIG_CHANGED,
                null
        ));
    }

    public void publishUserInvalidation(Long userId) {
        publish(new FeatureInvalidationEvent(
                InvalidationType.SUBSCRIPTION_CHANGED,
                userId
        ));
    }

    private void publish(FeatureInvalidationEvent event) {
        redisTemplate.convertAndSend(
                RedisChannels.FEATURE_INVALIDATION,
                event
        );
    }
}
