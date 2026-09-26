package com.paymentplatform.organization.infrastructure.http;

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
import java.util.UUID;

/**
 * Client HTTP vers l'identity-service (endpoints internes) pour résoudre
 * les noms d'utilisateurs et lister ceux d'une organisation.
 */
@Component
public class IdentityClient {

    private static final Logger log = LoggerFactory.getLogger(IdentityClient.class);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Appels directs au service, PAS via le gateway : les routes
    // internal/** exigent un JWT au gateway (B3), que le serveur n'a pas.
    @Value("${app.identity-service.url:localhost}")
    private String identityUrl;

    @Value("${app.identity-service.port:8082}")
    private int identityPort;

    @Value("${app.internal-secret}")
    private String internalSecret;

    /**
     * Résout le nom complet d'un utilisateur via l'endpoint interne.
     *
     * @param userId identifiant de l'utilisateur
     * @return prénom + nom, ou nul si introuvable ou en panne
     */
    public String resolveUserName(UUID userId) {
        if (userId == null) return null;
        try {
            JsonNode json = get("/api/internal/users/" + userId);
            if (json == null) return null;
            String firstName = json.has("firstName") ? json.get("firstName").asText() : "";
            String lastName = json.has("lastName") ? json.get("lastName").asText() : "";
            return (firstName + " " + lastName).trim();
        } catch (Exception e) {
            log.warn("Impossible de résolver le nom de l'utilisateur {}: {}", userId, e.getMessage());
            return null;
        }
    }

    /**
     * Récupère la fiche brute d'un utilisateur via l'endpoint interne.
     *
     * @param userId identifiant de l'utilisateur
     * @return nœud JSON de l'utilisateur, ou nul si introuvable ou en panne
     */
    public com.fasterxml.jackson.databind.JsonNode getUserById(UUID userId) {
        if (userId == null) return null;
        try {
            return get("/api/internal/users/" + userId);
        } catch (Exception e) {
            log.warn("Impossible de récupérer l'utilisateur {}: {}", userId, e.getMessage());
            return null;
        }
    }

    /**
     * Utilisateurs d'une organisation via l'endpoint interne (authentifié par
     * X-Internal-Token — l'ancien header X-System-Admin décoratif recevait 401,
     * ce qui vidait le sélecteur « Personne qui a reçu la livraison »).
     */
    public com.fasterxml.jackson.databind.JsonNode getUsersByOrganization(UUID organizationId) {
        if (organizationId == null) return null;
        try {
            return get("/api/internal/users?organizationId=" + organizationId);
        } catch (Exception e) {
            log.warn("Impossible de récupérer les utilisateurs de l'org {}: {}", organizationId, e.getMessage());
            return null;
        }
    }

    private JsonNode get(String path) throws Exception {
        String targetUri = "http://" + identityUrl + ":" + identityPort + path;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(targetUri))
                .header("X-Internal-Token", internalSecret)
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return objectMapper.readTree(response.body());
        }
        log.warn("Appel interne identity {} -> HTTP {}", path, response.statusCode());
        return null;
    }
}
