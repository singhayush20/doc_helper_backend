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

    private static final Duration HOME_TTL = Duration.ofMinutes(5);

    @SuppressWarnings("unchecked")
    public List<FeatureResponse> getCachedHomeFeatures(Long userId) {
        return (List<FeatureResponse>) redisTemplate.opsForValue()
                .get(RedisKeys.homeFeatures(userId));
    }

    public void cacheHomeFeatures(
            Long userId,
            List<FeatureResponse> response
    ) {
        redisTemplate.opsForValue().set(
                RedisKeys.homeFeatures(userId),
                response,
                HOME_TTL
        );
    }

    public void evictHomeFeatures(Long userId) {
        redisTemplate.delete(
                RedisKeys.homeFeatures(userId)
        );
    }
}
