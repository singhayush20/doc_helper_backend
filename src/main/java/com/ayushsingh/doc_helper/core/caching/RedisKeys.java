package com.ayushsingh.doc_helper.core.caching;

import com.ayushsingh.doc_helper.features.user_activity.entity.UserActivityGroup;

public final class RedisKeys {

    private RedisKeys() {
    }

    public static final String FEATURE_CACHE_VERSION_KEY =
            "features:cache:version";

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

    public static String productFeatureKey(
            Long userId,
            long version
    ) {
        return "features:v%d:product:user:%d"
                .formatted(version, userId);
    }
}

