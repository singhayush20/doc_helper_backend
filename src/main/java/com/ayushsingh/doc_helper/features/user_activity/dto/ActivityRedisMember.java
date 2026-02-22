package com.ayushsingh.doc_helper.features.user_activity.dto;

import com.ayushsingh.doc_helper.features.user_activity.entity.ActivityTargetType;

public final class ActivityRedisMember {

    private ActivityRedisMember() {}

    public static String toMember(ActivityTarget target) {
        return target.type().name() + ":" + target.id();
    }

    public static ActivityTarget parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        if (raw.chars().allMatch(Character::isDigit)) {
            return new ActivityTarget(ActivityTargetType.USER_DOC, Long.valueOf(raw));
        }

        String[] parts = raw.split(":", 2);
        if (parts.length != 2) {
            return null;
        }

        try {
            ActivityTargetType type = ActivityTargetType.valueOf(parts[0]);
            Long id = Long.valueOf(parts[1]);
            return new ActivityTarget(type, id);
        } catch (Exception e) {
            return null;
        }
    }
}
