package com.paymentplatform.notification.infrastructure.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resout les destinataires (utilisateurs et emails) d'une organisation via
 * l'endpoint interne d'identity-service. Cache 5 minutes par organisation.
 */
@Component
public class IdentityRecipientClient {

    private static final Logger log = LoggerFactory.getLogger(IdentityRecipientClient.class);
    private static final long CACHE_TTL_MS = 5 * 60 * 1000L;
    private static final int MAX_CACHED_ORGANIZATIONS = 1000;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();
    private final ObjectMapper objectMapper;
    private final Map<UUID, CacheEntry> cache = new ConcurrentHashMap<>();

    @Value("${app.identity-service.url:localhost}")
    private String identityUrl;

    @Value("${app.identity-service.port:8082}")
    private String identityPort;

    @Value("${app.internal-secret:local-internal-secret-change-me}")
    private String internalSecret;

    public IdentityRecipientClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public record Recipient(UUID id, String email, List<String> roles) {
    }

    public Optional<String> emailForOrganizationRole(UUID organizationId, String role) {
        List<Recipient> recipients = recipients(organizationId);
        Optional<String> byRole = recipients.stream()
                .filter(recipient -> recipient.roles().contains(role))
                .map(Recipient::email)
                .filter(email -> email != null && !email.isBlank())
                .findFirst();
        return byRole.or(() -> recipients.stream()
                .map(Recipient::email)
                .filter(email -> email != null && !email.isBlank())
                .findFirst());
    }

    public List<Recipient> recipients(UUID organizationId) {
        if (organizationId == null) {
            return List.of();
        }
        long now = System.currentTimeMillis();
        CacheEntry cached = cache.get(organizationId);
        if (cached != null && now - cached.loadedAt() < CACHE_TTL_MS) {
            return cached.recipients();
        }
        List<Recipient> recipients = fetch(organizationId);
        if (cache.size() >= MAX_CACHED_ORGANIZATIONS) {
            cache.clear();
        }
        cache.put(organizationId, new CacheEntry(recipients, now));
        return recipients;
    }

    private List<Recipient> fetch(UUID organizationId) {
        try {
            String targetUri = "http://" + identityUrl + ":" + identityPort
                    + "/api/internal/users?organizationId=" + organizationId;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(targetUri))
                    .header("X-Internal-Token", internalSecret)
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Recipients lookup failed for organization {}: status={}",
                        organizationId, response.statusCode());
                return List.of();
            }
            List<Recipient> recipients = new ArrayList<>();
            for (JsonNode node : objectMapper.readTree(response.body())) {
                UUID id = node.hasNonNull("id") ? UUID.fromString(node.get("id").asText()) : null;
                String email = node.hasNonNull("email") ? node.get("email").asText() : null;
                List<String> roles = new ArrayList<>();
                if (node.has("roles") && node.get("roles").isArray()) {
                    node.get("roles").forEach(role -> roles.add(role.asText()));
                }
                recipients.add(new Recipient(id, email, List.copyOf(roles)));
            }
            return List.copyOf(recipients);
        } catch (Exception e) {
            log.warn("Recipients lookup failed for organization {}: {}", organizationId, e.getMessage());
            return List.of();
        }
    }

    private record CacheEntry(List<Recipient> recipients, long loadedAt) {
    }
}
