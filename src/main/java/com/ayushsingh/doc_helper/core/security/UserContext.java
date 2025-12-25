package com.ayushsingh.doc_helper.core.security;

import com.ayushsingh.doc_helper.features.auth.entity.AuthUser;

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
