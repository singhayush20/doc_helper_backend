package com.ayushsingh.doc_helper.core.security;

import java.io.IOException;

import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.ayushsingh.doc_helper.features.auth.entity.AuthUser;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class UserContextFilter extends OncePerRequestFilter {

    private static final String USER_ID = "userId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof AuthUser authUser) {
                UserContext.setCurrentUser(authUser);
                Long userId = authUser.getUser() != null ? authUser.getUser().getId() : null;
                MDC.put(USER_ID, userId != null ? userId.toString() : "null");
            } else {
                MDC.put(USER_ID, "null");
            }

            filterChain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }
}
