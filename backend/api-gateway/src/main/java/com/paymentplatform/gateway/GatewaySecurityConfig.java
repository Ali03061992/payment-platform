package com.paymentplatform.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import java.time.Instant;
import java.util.Map;

@Configuration
@EnableWebSecurity
public class GatewaySecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/register",
                                "/api/auth/refresh",
                                "/actuator/health",
                                "/actuator/info"
                        ).permitAll()
                        .anyRequest().permitAll())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, e) -> {
                            response.setStatus(401);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            objectMapper.writeValue(response.getWriter(), Map.of(
                                    "timestamp", Instant.now().toString(),
                                    "status", 401,
                                    "error", "UNAUTHORIZED",
                                    "message", "Authentification requise",
                                    "path", request.getRequestURI()));
                        }));
        return http.build();
    }
}
