package com.ayushsingh.doc_helper.config.security;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import com.ayushsingh.doc_helper.commons.constants.AuthConstants;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableWebSecurity

@EnableMethodSecurity
public class SecurityConfig {

    private final FirebaseAuthFilter firebaseAuthFilter;
    private final FirebaseAuthenticationProvider firebaseAuthenticationProvider;
    private final HandlerExceptionResolver resolver;

    public SecurityConfig(FirebaseAuthFilter firebaseAuthFilter,
            FirebaseAuthenticationProvider firebaseAuthenticationProvider,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.firebaseAuthFilter = firebaseAuthFilter;
        this.firebaseAuthenticationProvider = firebaseAuthenticationProvider;
        this.resolver = resolver;
    }

    @Bean
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
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
                                "/api/v1/auth/**",
                                "/error")
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
            resolver.resolveException(request, response, null, authException);
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
