package com.ayushsingh.doc_helper.features.user_activity.service;

import com.ayushsingh.doc_helper.features.user_activity.dto.UserActivityResolution;
import com.ayushsingh.doc_helper.features.user_activity.dto.UserActivityWriteRequest;
import com.ayushsingh.doc_helper.features.user_activity.entity.UserActivity;
import com.ayushsingh.doc_helper.features.user_activity.entity.UserActivityGroup;
import com.ayushsingh.doc_helper.features.user_activity.entity.UserActivityMetadata;
import com.ayushsingh.doc_helper.features.user_activity.entity.UserActivityType;
import com.ayushsingh.doc_helper.features.user_activity.repository.UserActivityRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class UserActivityRecorder {

    private final UserActivityRepository repository;
    private final UserActivityRedisGateway redis;
    private final UserActivityRedisBuffer activityRedisBuffer;

    public UserActivityRecorder(
            UserActivityRepository repository,
            UserActivityRedisGateway redis,
            UserActivityRedisBuffer redisBuffer
    ) {
        this.repository = repository;
        this.redis = redis;
        this.activityRedisBuffer = redisBuffer;
    }

    @Transactional
    public void record(
            Long userId,
            Long documentId,
            UserActivityType action
    ) {
        UserActivityGroup group = UserActivityMetadata.groupOf(action);

        boolean allowed =
                redis.allowByDebounce(
                        userId,
                        documentId,
                        group,
                        debounceWindow(group)
                );

        UserActivity existing =
                repository.findByUserIdAndDocumentId(userId, documentId)
                        .orElse(null);

        UserActivityResolution resolution =
                UserActivityResolver.resolve(existing, action);

        if (!resolution.updateLastAction()) {
            return;
        }

        Instant now = Instant.now();

        UserActivity activity =
                existing != null ? existing : new UserActivity();

        activity.setUserId(userId);
        activity.setDocumentId(documentId);
        activity.setLastAction(action);
        activity.setLastActionAt(now);

        if (resolution.promoteDominant()) {
            activity.setDominantActivity(action);
            activity.setDominantAt(now);
        }

        activityRedisBuffer.buffer(
                new UserActivityWriteRequest(
                        activity.getUserId(),
                        activity.getDocumentId(),
                        activity.getDominantActivity(),
                        activity.getLastAction(),
                        activity.getDominantAt(),
                        activity.getLastActionAt(),
                        activity.getMetadata()
                )
        );

        if (resolution.updateRedis() && allowed) {
            redis.updateRecentDocuments(userId, documentId, now);
        }
    }

    private Duration debounceWindow(UserActivityGroup group) {
        return switch (group) {
            case ENGAGEMENT_LOW -> Duration.ofSeconds(10);
            case ENGAGEMENT_HIGH -> Duration.ofSeconds(30);
            case PROCESSING -> Duration.ZERO;
        };
    }
}

