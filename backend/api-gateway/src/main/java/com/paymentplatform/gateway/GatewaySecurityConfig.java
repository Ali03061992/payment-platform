package com.paymentplatform.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.Instant;
import java.util.Map;

@Configuration
@EnableWebSecurity
public class GatewaySecurityConfig {

    private final IpWhitelistFilter ipWhitelistFilter;
    private final JwtValidationFilter jwtValidationFilter;

    public GatewaySecurityConfig(IpWhitelistFilter ipWhitelistFilter,
                                 JwtValidationFilter jwtValidationFilter) {
        this.ipWhitelistFilter = ipWhitelistFilter;
        this.jwtValidationFilter = jwtValidationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        http
                .addFilterBefore(ipWhitelistFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtValidationFilter, UsernamePasswordAuthenticationFilter.class)
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // B3 : deny-by-default. Plus de "/api/**".permitAll() :
                        // toute route API exige une authentification (posée dans le
                        // SecurityContext par JwtValidationFilter, y compris le
                        // stream SSE via token en query param). Seules les routes
                        // réellement publiques restent ouvertes.
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/register",
                                "/api/auth/refresh",
                                "/api/auth/password-setup/**",
                                "/actuator/health",
                                "/actuator/info",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/webjars/**"
                        ).permitAll()
                        .anyRequest().authenticated())
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
                        })
                        .accessDeniedHandler((request, response, e) -> {
                            response.setStatus(403);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            objectMapper.writeValue(response.getWriter(), Map.of(
                                    "timestamp", Instant.now().toString(),
                                    "status", 403,
                                    "error", "FORBIDDEN",
                                    "message", "Accès refusé",
                                    "path", request.getRequestURI()));
                        }));
        return http.build();
    }

    /**
     * B3 : JwtValidationFilter ne s'exécute QUE dans la chaîne Spring Security
     * (ordre garanti avant l'autorisation). Sans cela, son auto-enregistrement
     * comme filtre servlet ferait double exécution.
     */
    @Bean
    public FilterRegistrationBean<JwtValidationFilter> jwtValidationFilterRegistration(
            JwtValidationFilter filter) {
        FilterRegistrationBean<JwtValidationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
