package com.paymentplatform.gateway;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate limiting par IP avec fenêtre glissante d'une minute.
 * Protège le gateway contre les abus et les attaques DDoS.
 *
 * <p>B2 : les endpoints d'authentification (login/register/refresh) ont un bucket
 * strict dédié (défaut 10/min/IP) pour freiner le brute-force. Les autres routes
 * gardent le bucket général. Les réponses 429 portent {@code Retry-After: 60}
 * (backoff progressif côté client — pas de sleep serveur pour ne pas épuiser
 * les threads Tomcat).</p>
 */
@Component
@Order(3)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final int maxRequestsPerMinute;
    private final int maxAuthRequestsPerMinute;
    private final ConcurrentHashMap<String, SlidingWindowCounter> counters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, SlidingWindowCounter> authCounters = new ConcurrentHashMap<>();

    public RateLimitFilter(@Value("${app.gateway.rate-limit-per-minute:120}") int maxRequestsPerMinute,
                           @Value("${app.gateway.rate-limit-auth-per-minute:10}") int maxAuthRequestsPerMinute) {
        this.maxRequestsPerMinute = maxRequestsPerMinute;
        this.maxAuthRequestsPerMinute = maxAuthRequestsPerMinute;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String clientIp = getClientIp(request);
        String path = request.getRequestURI();

        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || isExcludedPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (isAuthPath(path)) {
            SlidingWindowCounter counter = authCounters.computeIfAbsent(clientIp, k -> new SlidingWindowCounter());
            int count = counter.incrementAndGet();
            response.setHeader("X-RateLimit-Limit", String.valueOf(maxAuthRequestsPerMinute));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, maxAuthRequestsPerMinute - count)));
            if (count > maxAuthRequestsPerMinute) {
                log.warn("Auth rate limit exceeded for IP {} ({} auth requests in 1 minute, path {})",
                        clientIp, count, path);
                reject(response, maxAuthRequestsPerMinute);
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }

        SlidingWindowCounter counter = counters.computeIfAbsent(clientIp, k -> new SlidingWindowCounter());
        int count = counter.incrementAndGet();

        if (count > maxRequestsPerMinute) {
            log.warn("Rate limit exceeded for IP {} ({} requests in 1 minute)", clientIp, count);
            reject(response, maxRequestsPerMinute);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response, int limit) throws IOException {
        response.setStatus(429);
        response.setHeader("Retry-After", "60");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(Map.of(
                "timestamp", Instant.now().toString(),
                "status", 429,
                "error", "TOO_MANY_REQUESTS",
                "message", "Trop de requêtes. Limite: " + limit + " requêtes par minute.")));
    }

    private boolean isAuthPath(String path) {
        return path.startsWith("/api/auth/login")
                || path.startsWith("/api/auth/register")
                || path.startsWith("/api/auth/refresh")
                || path.startsWith("/api/auth/logout");
    }

    private boolean isExcludedPath(String path) {
        return path.startsWith("/actuator/")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs");
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Compteur avec fenêtre glissante d'une minute, nettoyage automatique.
     */
    private static class SlidingWindowCounter {
        private volatile long windowStart = System.currentTimeMillis();
        private final AtomicInteger count = new AtomicInteger(0);

        int incrementAndGet() {
            long now = System.currentTimeMillis();
            if (now - windowStart > 60_000) {
                synchronized (this) {
                    if (now - windowStart > 60_000) {
                        windowStart = now;
                        count.set(0);
                    }
                }
            }
            return count.incrementAndGet();
        }
    }
}
