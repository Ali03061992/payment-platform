package com.paymentplatform.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

@Component
public class JwtValidationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtValidationFilter.class);

    private final String jwtSecret;

    public JwtValidationFilter(@Value("${app.security.secret}") String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        if (isPublicPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = null;
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        } else if (path.equals("/api/notifications/stream")) {
            String query = request.getQueryString();
            if (query != null) {
                for (String param : query.split("&")) {
                    String[] kv = param.split("=", 2);
                    if (kv.length == 2 && "token".equals(kv[0])) {
                        token = java.net.URLDecoder.decode(kv[1], java.nio.charset.StandardCharsets.UTF_8);
                        break;
                    }
                }
            }
        }

        if (token == null) {
            sendError(response, 401, "Token d'authentification manquant", path);
            return;
        }

        if (!validateJwt(token)) {
            sendError(response, 401, "Token invalide ou expiré", path);
            return;
        }

        String finalToken = token;
        HttpServletRequest wrappedRequest = new HttpServletRequestWrapper(request) {
            @Override
            public String getHeader(String name) {
                if ("Authorization".equalsIgnoreCase(name)) {
                    return "Bearer " + finalToken;
                }
                return super.getHeader(name);
            }
        };

        filterChain.doFilter(wrappedRequest, response);
    }

    private boolean isPublicPath(String path) {
        return path.startsWith("/api/auth/login")
                || path.startsWith("/api/auth/register")
                || path.startsWith("/api/auth/refresh")
                || path.startsWith("/api/organizations/internal/")
                || path.startsWith("/api/internal/")
                || path.startsWith("/actuator/")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs");
    }

    private boolean validateJwt(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return false;

            String header = parts[0];
            String payload = parts[1];
            String signature = parts[2];

            byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(keyBytes, "HmacSHA256"));

            String expectedSignature = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal((header + "." + payload).getBytes(StandardCharsets.UTF_8)));

            if (!java.security.MessageDigest.isEqual(
                    signature.getBytes(StandardCharsets.UTF_8),
                    expectedSignature.getBytes(StandardCharsets.UTF_8))) {
                log.warn("JWT signature mismatch");
                return false;
            }

            String payloadJson = new String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8);
            var payloadNode = new ObjectMapper().readTree(payloadJson);

            if (payloadNode.has("exp")) {
                long exp = payloadNode.get("exp").asLong();
                if (System.currentTimeMillis() / 1000 > exp) {
                    log.warn("JWT expired");
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return false;
        }
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
