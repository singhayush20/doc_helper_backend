package com.ayushsingh.doc_helper.features.user_activity.dto;

import com.ayushsingh.doc_helper.features.user_activity.entity.ActivityTargetType;
import com.ayushsingh.doc_helper.features.user_activity.entity.UserActivityType;

import java.time.Instant;
import java.util.Map;

public record UserActivityWriteRequest(
        Long userId,
        ActivityTargetType targetType,
        Long targetId,
        UserActivityType dominantActivity,
        UserActivityType lastAction,
        Instant dominantAt,
        Instant lastActionAt,
        Map<String, Object> metadata
) {}
