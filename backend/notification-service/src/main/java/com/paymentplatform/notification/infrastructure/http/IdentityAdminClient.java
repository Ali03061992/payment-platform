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
import java.util.UUID;

/**
 * Résout les identifiants des administrateurs système via l'endpoint interne
 * d'identity-service (GET /api/internal/users/by-role?role=SYSTEM_ADMIN,
 * authentifié par X-Internal-Token). Résultat mis en cache 5 minutes.
 */
@Component
public class IdentityAdminClient {

    private static final Logger log = LoggerFactory.getLogger(IdentityAdminClient.class);
    private static final long CACHE_TTL_MS = 5 * 60 * 1000L;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();
    private final ObjectMapper objectMapper;

    @Value("${app.identity-service.url:localhost}")
    private String identityUrl;

    @Value("${app.identity-service.port:8082}")
    private String identityPort;

    @Value("${app.internal-secret:local-internal-secret-change-me}")
    private String internalSecret;

    private volatile List<UUID> cachedAdminIds = List.of();
    private volatile long cachedAt = 0L;

    public IdentityAdminClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<UUID> systemAdminIds() {
        long now = System.currentTimeMillis();
        if (now - cachedAt < CACHE_TTL_MS) {
            return cachedAdminIds;
        }
        List<UUID> ids = fetchSystemAdminIds();
        cachedAdminIds = ids;
        cachedAt = now;
        return ids;
    }

    private List<UUID> fetchSystemAdminIds() {
        try {
            String targetUri = "http://" + identityUrl + ":" + identityPort
                    + "/api/internal/users/by-role?role=SYSTEM_ADMIN";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(targetUri))
                    .header("X-Internal-Token", internalSecret)
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Liste des admins impossible: status={}", response.statusCode());
                return List.of();
            }
            List<UUID> ids = new ArrayList<>();
            for (JsonNode node : objectMapper.readTree(response.body())) {
                JsonNode id = node.get("id");
                if (id != null && !id.isNull()) {
                    ids.add(UUID.fromString(id.asText()));
                }
            }
            return ids;
        } catch (Exception e) {
            log.error("Liste des admins impossible", e);
            return List.of();
        }
    }
}
