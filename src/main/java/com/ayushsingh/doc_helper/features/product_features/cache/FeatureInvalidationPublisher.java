package com.ayushsingh.doc_helper.features.product_features.cache;

import com.ayushsingh.doc_helper.core.pub_sub.RedisChannels;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FeatureInvalidationPublisher {

    private final RedisTemplate<String, Object> redisTemplate;

    /* Feature list changed */
    public void publishFeatureListInvalidation() {
        publish(new FeatureInvalidationEvent(
                InvalidationType.FEATURE_LIST_CHANGED,
                null,
                null
        ));
    }

    /* UI changed */
    public void publishUIInvalidation(
            Long featureId,
            String screen,
            Integer featureUIVersion
    ) {
        publish(new FeatureInvalidationEvent(
                InvalidationType.FEATURE_UI_CHANGED,
                null,
                new UIInvalidationPayload(
                        featureId, screen, featureUIVersion
                )
        ));
    }

    /* Subscription-specific */
    public void publishUserInvalidation(Long userId) {
        publish(new FeatureInvalidationEvent(
                InvalidationType.SUBSCRIPTION_CHANGED,
                userId,
                null
        ));
    }

    private void publish(FeatureInvalidationEvent event) {
        redisTemplate.convertAndSend(
                RedisChannels.FEATURE_INVALIDATION,
                event
        );
    }
}
