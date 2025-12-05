package com.ayushsingh.doc_helper.core.logging;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class MDCFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID = "requestId";
    private static final String HTTP_METHOD = "httpMethod";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException {

        try {
            MDC.put(REQUEST_ID, UUID.randomUUID().toString());

            MDC.put(HTTP_METHOD, request.getMethod());

            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
