package com.ayushsingh.doc_helper.features.user_activity.service;

import com.ayushsingh.doc_helper.features.user_activity.dto.ActivityTarget;
import com.ayushsingh.doc_helper.features.user_activity.dto.UserActivityResolution;
import com.ayushsingh.doc_helper.features.user_activity.dto.UserActivityWriteRequest;
import com.ayushsingh.doc_helper.features.user_activity.entity.UserActivity;
import com.ayushsingh.doc_helper.features.user_activity.entity.UserActivityGroup;
import com.ayushsingh.doc_helper.features.user_activity.entity.UserActivityMetadata;
import com.ayushsingh.doc_helper.features.user_activity.entity.UserActivityType;
import com.ayushsingh.doc_helper.features.user_activity.repository.UserActivityRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class UserActivityRecorder {

    private final UserActivityRepository repository;
    private final UserActivityRedisGateway userActivityRedisGateway;
    private final UserActivityRedisBuffer activityRedisBuffer;

    @Transactional
    public void record(
            Long userId,
            ActivityTarget target,
            UserActivityType action
    ) {
        UserActivityGroup activityGroup = UserActivityMetadata.groupOf(action);

        // Check if the debounce window has passed or not before updating for the next event. This
        // prevents repetitive updates in a very short time span
        boolean isUpdateRequired =
                userActivityRedisGateway.allowByDebounce(
                        userId,
                        target,
                        activityGroup,
                        debounceWindow(activityGroup)
                );

        UserActivity existing =
                repository.findByUserIdAndTargetTypeAndTargetId(userId, target.type(), target.id())
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
        activity.setTargetType(target.type());
        activity.setTargetId(target.id());
        activity.setLastAction(action);
        activity.setLastActionAt(now);

        if (resolution.promoteDominant()) {
            activity.setDominantActivity(action);
            activity.setDominantAt(now);
        }

        activityRedisBuffer.buffer(
                new UserActivityWriteRequest(
                        activity.getUserId(),
                        activity.getTargetType(),
                        activity.getTargetId(),
                        activity.getDominantActivity(),
                        activity.getLastAction(),
                        activity.getDominantAt(),
                        activity.getLastActionAt(),
                        activity.getMetadata()
                )
        );

        if (resolution.updateRedis() && isUpdateRequired) {
            userActivityRedisGateway.updateRecentDocuments(userId, target, now);
        }
    }

    private Duration debounceWindow(UserActivityGroup group) {
        return switch (group) {
            case ENGAGEMENT_LOW -> Duration.ofSeconds(10);
            case ENGAGEMENT_HIGH -> Duration.ofSeconds(30);
            case PROCESSING -> Duration.ofSeconds(1);
        };
    }
}

