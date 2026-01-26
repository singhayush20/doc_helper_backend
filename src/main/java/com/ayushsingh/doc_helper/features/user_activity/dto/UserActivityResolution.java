package com.ayushsingh.doc_helper.features.user_activity.dto;

public record UserActivityResolution(
        boolean promoteDominant,
        boolean updateLastAction,
        boolean updateRedis,
        boolean scheduleDbWrite
) {}

