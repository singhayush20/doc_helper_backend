package com.ayushsingh.doc_helper.features.user_activity.service;

import com.ayushsingh.doc_helper.core.caching.RedisKeys;
import com.ayushsingh.doc_helper.features.user_activity.entity.UserActivityGroup;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class UserActivityRedisGateway {

    private final RedisTemplate<String, String> redis;

    public UserActivityRedisGateway(RedisTemplate<String, String> redis) {
        this.redis = redis;
    }

    public boolean allowByDebounce(
            Long userId,
            Long documentId,
            UserActivityGroup group,
            Duration ttl
    ) {
        String key = RedisKeys.debounceKey(userId, documentId, group);
        Boolean ok = redis.opsForValue()
                .setIfAbsent(key, "1", ttl);

        return Boolean.TRUE.equals(ok);
    }

    public void updateRecentDocuments(
            Long userId,
            Long documentId,
            Instant at
    ) {
        String key = RedisKeys.recentDocsKey(userId);

        redis.opsForZSet().add(
                key,
                documentId.toString(),
                at.toEpochMilli()
        );

        redis.opsForZSet()
                .removeRange(key, 0, -6);
    }
}

