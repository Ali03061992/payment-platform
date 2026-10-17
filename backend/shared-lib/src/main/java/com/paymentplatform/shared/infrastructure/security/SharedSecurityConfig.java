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
                                "/api/auth/logout",
                                "/api/auth/password-setup/**",
                                "/api/auth/password-reset/**",
                                // WebAuthn : la demande d'options et la connexion passkey sont
                                // publiques (rate-limitées au gateway) ; l'enregistrement et la
                                // gestion des appareils restent protégés par le JWT.
                                "/api/auth/webauthn/login", "/api/auth/webauthn/login/options",
                                "/api/organizations/internal/**",
                                "/api/internal/**",
                                // SSE : nginx contourne le gateway (pas de header possible via
                                // EventSource) ; le contrôleur valide lui-même le JWT signé
                                // en query (?token=, signature HMAC + expiry).
                                "/api/notifications/stream",
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