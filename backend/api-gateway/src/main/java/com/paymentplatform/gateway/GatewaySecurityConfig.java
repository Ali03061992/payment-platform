package com.paymentplatform.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Configuration de sécurité du gateway : chaîne stateless deny-by-default,
 * filtres IP et JWT ordonnés avant l'authentification Spring.
 */
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

    /**
     * Construit la chaîne de sécurité stateless (CSRF désactivé, 401/403 JSON).
     *
     * @param http configuration HTTP à personnaliser
     * @param objectMapper sérialiseur des réponses d'erreur
     * @return chaîne de filtres configurée
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        http
                .addFilterBefore(ipWhitelistFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtValidationFilter, UsernamePasswordAuthenticationFilter.class)
                .cors(Customizer.withDefaults())
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
                                "/api/auth/password-reset/**",
                                "/api/auth/refresh",
                                "/api/auth/logout",
                                "/api/auth/password-setup/**",
                                "/api/auth/webauthn/login",
                                "/api/auth/webauthn/login/options",
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
     * Origines autorisées, limitées aux domaines publics configurés. Les
     * credentials sont autorisés pour le cookie refresh HttpOnly ; une wildcard
     * n'est donc jamais utilisée.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.gateway.allowed-origins}") String allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With"));
        config.setExposedHeaders(List.of("X-RateLimit-Limit", "X-RateLimit-Remaining", "Retry-After"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
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
