package com.ayushsingh.doc_helper.features.product_features.service.service_impl;

import com.ayushsingh.doc_helper.core.caching.RedisKeys;
import com.ayushsingh.doc_helper.features.product_features.dto.FeatureResponse;
import com.ayushsingh.doc_helper.features.product_features.service.FeatureCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class FeatureCacheServiceImpl implements FeatureCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final Duration TTL = Duration.ofMinutes(5);

    @Override
    public void bumpGlobalVersion() {
        redisTemplate.opsForValue()
                .increment(RedisKeys.FEATURE_LIST_CACHE_VERSION_KEY);
    }

    private long getCurrentVersion() {
        Long version = (Long) redisTemplate.opsForValue()
                .get(RedisKeys.FEATURE_LIST_CACHE_VERSION_KEY);
        return version != null ? version : 1L;
    }

    @Override
    public FeatureResponse getCachedProductFeatures(Long userId) {
        long version = getCurrentVersion();
        return (FeatureResponse) redisTemplate.opsForValue()
                .get(RedisKeys.productFeatureKey(userId, version));
    }

    @Override
    public void cacheProductFeatures(Long userId, FeatureResponse response) {
        long version = getCurrentVersion();
        redisTemplate.opsForValue().set(
                RedisKeys.productFeatureKey(userId, version),
                response,
                TTL
        );
    }

    @Override
    public void evictProductFeatures(Long userId) {
        long version = getCurrentVersion();
        redisTemplate.delete(
                RedisKeys.productFeatureKey(userId, version)
        );
    }
}
