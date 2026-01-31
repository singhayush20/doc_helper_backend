package com.ayushsingh.doc_helper.core.caching;

import com.ayushsingh.doc_helper.features.user_activity.entity.UserActivityGroup;

public final class RedisKeys {

    private RedisKeys() {
    }

    public static String debounceKey(
            Long userId,
            Long documentId,
            UserActivityGroup group
    ) {
        return "activity:debounce:%d:%d:%s"
                .formatted(userId, documentId, group.name());
    }

    public static String recentDocsKey(Long userId) {
        return "recent:documents:%d".formatted(userId);
    }

    public static String homeFeatures(Long userId) {
        return "features:home:user:" + userId;
    }
}

