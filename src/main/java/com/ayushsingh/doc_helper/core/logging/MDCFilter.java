package com.ayushsingh.doc_helper.core.logging;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class MDCFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID = "requestId";
    private static final String USER_ID = "userId";
    private static final String HTTP_METHOD = "httpMethod";
    private static final String PATH = "path";
    private static final String STATUS = "status";
    private static final String DURATION_MS = "durationMs";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        Instant start = Instant.now();

        try {
            MDC.put(REQUEST_ID, UUID.randomUUID().toString());
            MDC.put(USER_ID, "null");
            MDC.put(HTTP_METHOD, request.getMethod());
            MDC.put(PATH, request.getRequestURI());

            filterChain.doFilter(request, response);
        } finally {
            Duration duration = Duration.between(start, Instant.now());
            MDC.put(STATUS, String.valueOf(response.getStatus()));
            MDC.put(DURATION_MS, String.valueOf(duration.toMillis()));
            log.info("event=http_request_complete");
            MDC.clear();
        }
    }
}
