package com.ayushsingh.doc_helper.features.user_activity.service;

import com.ayushsingh.doc_helper.core.caching.RedisKeys;
import com.ayushsingh.doc_helper.features.user_activity.dto.ActivityRedisMember;
import com.ayushsingh.doc_helper.features.user_activity.dto.ActivityTarget;
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

    /**
     * Checks and stores a debounce key for the given userId, documentId and activity group
     * If the key is found to be present, it means the debounce window has still not passed.
     * If the keys is not found, it means the debounce window has passed and the key is stored.
     */
    public boolean allowByDebounce(
            Long userId,
            ActivityTarget target,
            UserActivityGroup group,
            Duration ttl
    ) {
        String key = RedisKeys.debounceKey(userId, target.type(), target.id(), group);
        Boolean ok = redis.opsForValue()
                .setIfAbsent(key, "1", ttl);

        return Boolean.TRUE.equals(ok);
    }

    public void updateRecentDocuments(
            Long userId,
            ActivityTarget target,
            Instant at
    ) {
        String key = RedisKeys.recentDocsKey(userId);

        redis.opsForZSet().add(
                key,
                ActivityRedisMember.toMember(target),
                at.toEpochMilli()
        );

        // Remove the oldest documents and retain the newest 5
        redis.opsForZSet()
                .removeRange(key, 0, -6);
    }
}

