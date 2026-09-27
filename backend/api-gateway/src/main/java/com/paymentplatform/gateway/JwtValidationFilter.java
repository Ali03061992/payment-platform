package com.paymentplatform.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentplatform.shared.domain.security.PermissionCatalog;
import com.paymentplatform.shared.infrastructure.security.AuthenticatedUser;
import com.paymentplatform.shared.infrastructure.security.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Filtre gateway validant le JWT (header Bearer, ou token en query pour le SSE)
 * et alimentant le contexte Spring Security avec rôles et permissions.
 */
@Component
@Order(2)
public class JwtValidationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtValidationFilter.class);

    private final JwtService jwtService;

    public JwtValidationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
        log.info("JwtValidationFilter initialized with shared JwtService bean");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || isPublicPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = extractToken(request);

        if (token == null) {
            SecurityContextHolder.clearContext();
            sendError(response, 401, "Token d'authentification manquant", path);
            return;
        }

        try {
            AuthenticatedUser user = jwtService.parse(token);

            // B3 : alimente le SecurityContext pour que la chaîne Spring Security
            // (deny-by-default, sans "/api/**".permitAll()) authentifie la requête.
            List<SimpleGrantedAuthority> authorities = new ArrayList<>(user.roles().stream()
                    .map(SimpleGrantedAuthority::new)
                    .toList());
            authorities.addAll(PermissionCatalog.permissionsFor(user.roles()).stream()
                    .map(SimpleGrantedAuthority::new)
                    .toList());
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(user, null, authorities));

            String finalToken = token;
            AuthenticatedUser finalUser = user;
            HttpServletRequest wrappedRequest = new HttpServletRequestWrapper(request) {
                @Override
                public String getHeader(String name) {
                    if ("Authorization".equalsIgnoreCase(name)) {
                        return "Bearer " + finalToken;
                    }
                    return super.getHeader(name);
                }

                @Override
                public Object getAttribute(String name) {
                    if ("authenticatedUser".equals(name)) {
                        return finalUser;
                    }
                    return super.getAttribute(name);
                }
            };

            filterChain.doFilter(wrappedRequest, response);
        } catch (Exception e) {
            log.warn("JWT rejected for {}: {}", path, e.getMessage());
            SecurityContextHolder.clearContext();
            sendError(response, 401, "Token invalide ou expiré", path);
        }
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        if (request.getRequestURI().equals("/api/notifications/stream")) {
            String query = request.getQueryString();
            if (query != null) {
                for (String param : query.split("&")) {
                    String[] kv = param.split("=", 2);
                    if (kv.length == 2 && "token".equals(kv[0])) {
                        return java.net.URLDecoder.decode(kv[1], java.nio.charset.StandardCharsets.UTF_8);
                    }
                }
            }
        }
        return null;
    }

    private boolean isPublicPath(String path) {
        // B3 : les routes internal/** exigent désormais un JWT valide au gateway
        // (défense en profondeur : JWT + X-Internal-Token côté service appelé).
        // Les appels inter-services directs (hors gateway) ne sont pas impactés.
        return path.startsWith("/api/auth/login")
                || path.startsWith("/api/auth/register")
                || path.startsWith("/api/auth/refresh")
                || path.startsWith("/api/auth/logout")
                || path.startsWith("/api/auth/password-setup/")
                || path.startsWith("/actuator/")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs");
    }

    private void sendError(HttpServletResponse response, int status, String message, String path) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(new ObjectMapper().writeValueAsString(Map.of(
                "timestamp", Instant.now().toString(),
                "status", status,
                "error", status == 401 ? "UNAUTHORIZED" : "FORBIDDEN",
                "message", message,
                "path", path)));
    }
}
