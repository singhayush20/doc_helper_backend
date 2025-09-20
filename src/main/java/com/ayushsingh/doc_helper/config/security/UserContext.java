package com.ayushsingh.doc_helper.config.security;

import com.ayushsingh.doc_helper.features.auth.domain.AuthUser;

public class UserContext {

    private static final ThreadLocal<AuthUser> currentUser = new ThreadLocal<>();

    public static void setCurrentUser(AuthUser authUser) {
        currentUser.set(authUser);
    }

    public static AuthUser getCurrentUser() {
        return currentUser.get();
    }

    public static void clear() {
        currentUser.remove();
    }
}
