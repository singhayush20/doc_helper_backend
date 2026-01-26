package com.ayushsingh.doc_helper.features.user_activity.entity;

import java.util.Map;

public final class UserActivityMetadata {

    private UserActivityMetadata() {}

    private static final Map<UserActivityType, UserActivityGroup> GROUPS =
            Map.of(
                    UserActivityType.DOCUMENT_UPLOAD, UserActivityGroup.ENGAGEMENT_LOW,
                    UserActivityType.DOCUMENT_VIEW, UserActivityGroup.ENGAGEMENT_LOW,
                    UserActivityType.DOCUMENT_CHAT, UserActivityGroup.ENGAGEMENT_HIGH,
                    UserActivityType.DOCUMENT_LIVE_CHAT, UserActivityGroup.ENGAGEMENT_HIGH,
                    UserActivityType.DOCUMENT_OCR, UserActivityGroup.PROCESSING,
                    UserActivityType.DOCUMENT_SUMMARY, UserActivityGroup.PROCESSING
            );

    private static final Map<UserActivityType, Integer> PRECEDENCE =
            Map.of(
                    UserActivityType.DOCUMENT_LIVE_CHAT, 100,
                    UserActivityType.DOCUMENT_CHAT, 90,
                    UserActivityType.DOCUMENT_VIEW, 50,
                    UserActivityType.DOCUMENT_UPLOAD, 40,
                    UserActivityType.DOCUMENT_OCR, 30,
                    UserActivityType.DOCUMENT_SUMMARY, 20
            );

    public static UserActivityGroup groupOf(UserActivityType type) {
        return GROUPS.get(type);
    }

    public static int precedenceOf(UserActivityType type) {
        return PRECEDENCE.get(type);
    }
}

