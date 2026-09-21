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
import java.util.UUID;

@Component
public class IdentityClient {

    private static final Logger log = LoggerFactory.getLogger(IdentityClient.class);
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${IDENTITY_SERVICE_URL:localhost}")
    private String identityUrl;

    @Value("${IDENTITY_SERVICE_PORT:8081}")
    private String identityPort;

    public String resolveUserName(UUID userId) {
        if (userId == null) return null;
        try {
            String targetUri = "http://" + identityUrl + ":" + identityPort + "/api/users/" + userId;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(targetUri))
                    .header("X-System-Admin", "true")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonNode json = objectMapper.readTree(response.body());
                String firstName = json.has("firstName") ? json.get("firstName").asText() : "";
                String lastName = json.has("lastName") ? json.get("lastName").asText() : "";
                return (firstName + " " + lastName).trim();
            }
            return null;
        } catch (Exception e) {
            log.warn("Impossible de résolver le nom de l'utilisateur {}: {}", userId, e.getMessage());
            return null;
        }
    }

    public com.fasterxml.jackson.databind.JsonNode getUserById(UUID userId) {
        if (userId == null) return null;
        try {
            String targetUri = "http://" + identityUrl + ":" + identityPort + "/api/users/" + userId;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(targetUri))
                    .header("X-System-Admin", "true")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return objectMapper.readTree(response.body());
            }
            return null;
        } catch (Exception e) {
            log.warn("Impossible de récupérer l'utilisateur {}: {}", userId, e.getMessage());
            return null;
        }
    }

    public com.fasterxml.jackson.databind.JsonNode getUsersByOrganization(UUID organizationId) {
        if (organizationId == null) return null;
        try {
            String targetUri = "http://" + identityUrl + ":" + identityPort + "/api/users?organizationId=" + organizationId;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(targetUri))
                    .header("X-System-Admin", "true")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return objectMapper.readTree(response.body());
            }
            return null;
        } catch (Exception e) {
            log.warn("Impossible de récupérer les utilisateurs de l'org {}: {}", organizationId, e.getMessage());
            return null;
        }
    }
}
