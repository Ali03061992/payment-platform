package com.paymentplatform.identity.infrastructure.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Rend publics /api/auth/oauth et /api/auth/dev-login (comme /api/auth/login).
 * Complète la chaîne partagée (shared-lib, non modifiable) qui ne connaît que
 * login/register/refresh/logout : chaîne prioritaire dédiée aux deux nouvelles routes.
 */
@Configuration
public class OAuthPublicEndpointsConfig {

    @Bean
    @Order(0)
    public SecurityFilterChain oauthPublicChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/auth/oauth", "/api/auth/dev-login")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
