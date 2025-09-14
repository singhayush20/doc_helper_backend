package com.ayushsingh.doc_helper.commons.config.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class MDCFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID = "requestId";
    private static final String USER_ID = "userId";
    private static final String ENDPOINT = "endpoint";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        try {
            // Unique ID per request
            MDC.put(REQUEST_ID, UUID.randomUUID().toString());

            // Optionally, extract user ID from request/session/token
            String userId = request.getHeader("X-USER-ID"); // example
            if (userId != null) {
                MDC.put(USER_ID, userId);
            }

            // Log the endpoint
            MDC.put(ENDPOINT, request.getRequestURI());

            filterChain.doFilter(request, response);
        } finally {
            MDC.clear(); // very important to prevent memory leaks
        }
    }
}
