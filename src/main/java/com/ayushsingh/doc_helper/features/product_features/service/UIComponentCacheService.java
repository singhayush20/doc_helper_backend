package com.ayushsingh.doc_helper.features.product_features.service;

import com.ayushsingh.doc_helper.core.caching.RedisKeys;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class UIComponentCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final Duration TTL = Duration.ofMinutes(10);

    public JsonNode getCachedUI(
            Long featureId,
            String screen,
            Integer featureUIVersion
    ) {
        return (JsonNode) redisTemplate.opsForValue()
                .get(RedisKeys.featureUIKey(
                        featureId, screen, featureUIVersion));
    }

    public void cacheUI(
            Long featureId,
            String screen,
            Integer featureUIVersion,
            JsonNode uiJson
    ) {
        redisTemplate.opsForValue().set(
                RedisKeys.featureUIKey(
                        featureId, screen, featureUIVersion),
                uiJson,
                TTL
        );
    }

    public void evictUI(
            Long featureId,
            String screen,
            Integer featureUIVersion
    ) {
        redisTemplate.delete(
                RedisKeys.featureUIKey(
                        featureId, screen, featureUIVersion));
    }
}

