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

    public static final String FEATURE_LIST_CACHE_VERSION_KEY =
            "features:list:cache:version";

    public static String productFeatureKey(Long userId, long version) {
        return "features:list:v%d:user:%d".formatted(version, userId);
    }
    public static String featureUIKey(
            Long featureId,
            String screen,
            Integer featureUIVersion
    ) {
        return "feature-ui:%d:%s:%d"
                .formatted(featureId, screen, featureUIVersion);
    }
}

