package com.paymentplatform.shared.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentplatform.shared.infrastructure.web.ApiError;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.Instant;

/**
 * Configuration de sécurité partagée par les microservices (pas le Gateway,
 * qui désactive app.security.shared=false et fournit sa propre config).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@ConditionalOnProperty(name = "app.security.shared", havingValue = "true", matchIfMissing = true)
public class SharedSecurityConfig {

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService) {
        return new JwtAuthenticationFilter(jwtService);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter,
                                                   ObjectMapper objectMapper) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login", "/api/auth/register", "/api/auth/refresh",
                                "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/actuator/health").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, e) -> {
                            response.setStatus(401);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            objectMapper.writeValue(response.getWriter(), new ApiError(Instant.now(), 401,
                                    "UNAUTHORIZED", "Authentification requise", request.getRequestURI()));
                        })
                        .accessDeniedHandler((request, response, e) -> {
                            response.setStatus(403);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            objectMapper.writeValue(response.getWriter(), new ApiError(Instant.now(), 403,
                                    "FORBIDDEN", "Accès refusé", request.getRequestURI()));
                        }))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}