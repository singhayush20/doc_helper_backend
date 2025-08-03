package com.ayushsingh.doc_helper.commons.config.security;

import java.time.Instant;

import com.ayushsingh.doc_helper.commons.constants.AuthConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final FirebaseAuthFilter firebaseAuthFilter;
    private final FirebaseAuthenticationProvider firebaseAuthenticationProvider;

    public SecurityConfig(FirebaseAuthFilter firebaseAuthFilter,
                          FirebaseAuthenticationProvider firebaseAuthenticationProvider) {
        this.firebaseAuthFilter = firebaseAuthFilter;
        this.firebaseAuthenticationProvider = firebaseAuthenticationProvider;
    }

    @Bean
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(AuthConstants.AUTH_API_PATTERN,
                                "/swagger-ui/**",
                                "/webjars/**",
                                "/configuration/ui",
                                "/favicon.ico",
                                "/api/public/**",
                                "/configuration/security",
                                "/swagger-resources/**",
                                "/v3/api-docs/**",
                                "/v3/api-docs",
                                "/error"
                                )
                        .permitAll()
                        .anyRequest().authenticated())
                .authenticationProvider(firebaseAuthenticationProvider)
                .addFilterBefore(firebaseAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(firebaseAuthenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler()));

        return http.build();
    }

    @Bean
    public AuthenticationEntryPoint firebaseAuthenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            String jsonResponse = """
                    {
                        "success": false,
                        "error": {
                            "code": "UNAUTHORIZED",
                            "message": "Authentication required. Please provide a valid Firebase token.",
                            "timestamp": "%s"
                        }
                    }
                    """.formatted(Instant.now());

            response.getWriter().write(jsonResponse);
        };
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            String jsonResponse = """
                    {
                        "success": false,
                        "error": {
                            "code": "FORBIDDEN",
                            "message": "Insufficient privileges to access this resource.",
                            "timestamp": "%s"
                        }
                    }
                    """.formatted(Instant.now());

            response.getWriter().write(jsonResponse);
        };
    }
}
