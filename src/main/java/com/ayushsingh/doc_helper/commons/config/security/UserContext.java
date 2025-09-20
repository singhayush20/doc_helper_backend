package com.ayushsingh.doc_helper.commons.config.security;

import com.ayushsingh.doc_helper.features.auth.domain.AuthUser;

public class UserContext {

    private static final ThreadLocal<AuthUser> currentUser = new ThreadLocal<>();

    public static void setCurrentUserId(AuthUser authUser) {
        currentUser.set(authUser);
    }

    public static AuthUser getCurrentUserId() {
        return currentUser.get();
    }

    public static void clear() {
        currentUser.remove();
    }
}
