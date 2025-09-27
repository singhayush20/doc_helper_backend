package com.ayushsingh.doc_helper.config.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import com.ayushsingh.doc_helper.commons.constants.AuthConstants;
import com.ayushsingh.doc_helper.commons.exception_handling.ExceptionCodes;
import com.ayushsingh.doc_helper.commons.exception_handling.exceptions.BaseException;
import com.ayushsingh.doc_helper.features.auth.domain.AuthUser;
import com.ayushsingh.doc_helper.features.user.domain.User;
import com.ayushsingh.doc_helper.features.user.service.UserService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class FirebaseAuthFilter extends OncePerRequestFilter {

    private final FirebaseAuth firebaseAuth;
    private final UserService userService;
    private final HandlerExceptionResolver exceptionResolver;

    public FirebaseAuthFilter(FirebaseAuth firebaseAuth,
            UserService userService,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) {
        this.firebaseAuth = firebaseAuth;
        this.userService = userService;
        this.exceptionResolver = exceptionResolver;
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return true;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = getTokenFromRequest(request);

        if (token != null) {
            try {
                FirebaseToken decodedToken = firebaseAuth.verifyIdToken(token);
                String firebaseUid = decodedToken.getUid();
                User user = userService.findByFirebaseUid(firebaseUid);

                if (user != null) {
                    AuthUser authUser = new AuthUser(user);
                    FirebaseAuthenticationToken authentication = new FirebaseAuthenticationToken(authUser,
                            authUser.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    log.warn("User not found for Firebase UID: {}", firebaseUid);
                    SecurityContextHolder.clearContext();
                    exceptionResolver.resolveException(request, response, null,
                            new BaseException("User not found",
                                    ExceptionCodes.USER_NOT_FOUND));
                    return;
                }
            } catch (FirebaseAuthException | AuthenticationException e) {
                log.error("Firebase token verification failed: {}", e.getMessage());
                SecurityContextHolder.clearContext();
                exceptionResolver.resolveException(request, response, null,
                        new BaseException("User is unauthorized",
                                ExceptionCodes.FIREBASE_AUTH_EXCEPTION));
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AuthConstants.AUTHORIZATION_HEADER);
        if (bearerToken != null && bearerToken.startsWith(AuthConstants.BEARER)) {
            return bearerToken.substring(7);
        }
        return null;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        String method = request.getMethod();

        // Skip filter for public endpoints
        var skipFilter = (path.startsWith(AuthConstants.AUTH_API_PREFIX) && "POST".equals(method))
                || path.startsWith("/swagger-ui")
                || path.startsWith("/swagger-resources")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/webjars/")
                || path.equals("/swagger-ui/index.html")
                || path.endsWith(".js")
                || path.endsWith(".css")
                || path.endsWith(".html")
                || path.endsWith(".png")
                || path.endsWith(".ico")
                || path.endsWith(".map")
                || path.startsWith("/api/public/");

        return skipFilter;
    }
}
