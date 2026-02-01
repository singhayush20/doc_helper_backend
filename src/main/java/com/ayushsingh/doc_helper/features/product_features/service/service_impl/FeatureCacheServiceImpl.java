package com.ayushsingh.doc_helper.features.product_features.service.service_impl;

import com.ayushsingh.doc_helper.core.caching.RedisKeys;
import com.ayushsingh.doc_helper.features.product_features.dto.FeatureResponse;
import com.ayushsingh.doc_helper.features.product_features.service.FeatureCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FeatureCacheServiceImpl implements FeatureCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final Duration PRODUCT_FEATURE_TTL = Duration.ofMinutes(5);

    @SuppressWarnings("unchecked")
    @Override
    public List<FeatureResponse> getCachedProductFeatures(Long userId) {
        return (List<FeatureResponse>) redisTemplate.opsForValue()
                .get(RedisKeys.productFeatureKey(userId));
    }

    @Override
    public void cacheProductFeatures(
            Long userId,
            List<FeatureResponse> response
    ) {
        redisTemplate.opsForValue().set(
                RedisKeys.productFeatureKey(userId),
                response,
                PRODUCT_FEATURE_TTL
        );
    }

    @Override
    public void evictProductFeatures(Long userId) {
        redisTemplate.delete(
                RedisKeys.productFeatureKey(userId)
        );
    }
}
