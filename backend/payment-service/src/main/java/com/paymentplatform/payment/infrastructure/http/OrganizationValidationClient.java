package com.paymentplatform.payment.infrastructure.http;

import com.paymentplatform.shared.domain.exception.ConflictException;
import com.paymentplatform.shared.domain.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class OrganizationValidationClient {

    private static final Logger log = LoggerFactory.getLogger(OrganizationValidationClient.class);
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.gateway.base-url:http://localhost:8081}")
    private String gatewayBaseUrl;

    @Value("${app.internal-secret:dev-internal-secret-change-me}")
    private String internalSecret;

    public void validateShop(long shopId) {
        String url = gatewayBaseUrl + "/api/organizations/internal/" + shopId + "/status";
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("X-Internal-Token", internalSecret)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 404) {
                throw new NotFoundException("Boutique non trouvée : " + shopId);
            }
            if (response.statusCode() != 200) {
                throw new ConflictException("Erreur validation boutique : HTTP " + response.statusCode());
            }
            if (!response.body().contains("\"SHOP\"")) {
                throw new ConflictException("L'organisation " + shopId + " n'est pas une boutique");
            }
            if (response.body().contains("\"DISABLED\"")) {
                throw new ConflictException("La boutique " + shopId + " est désactivée");
            }
        } catch (NotFoundException | ConflictException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Impossible de valider la boutique {} via l'API Organization : {}", shopId, e.getMessage());
            throw new ConflictException("Service de validation des organisations indisponible");
        }
    }

    public void validateSupplier(long supplierId) {
        String url = gatewayBaseUrl + "/api/organizations/internal/" + supplierId + "/status";
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("X-Internal-Token", internalSecret)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 404) {
                throw new NotFoundException("Fournisseur non trouvé : " + supplierId);
            }
            if (response.statusCode() != 200) {
                throw new ConflictException("Erreur validation fournisseur : HTTP " + response.statusCode());
            }
            if (!response.body().contains("\"SUPPLIER\"")) {
                throw new ConflictException("L'organisation " + supplierId + " n'est pas un fournisseur");
            }
            if (response.body().contains("\"DISABLED\"")) {
                throw new ConflictException("Le fournisseur " + supplierId + " est désactivé");
            }
        } catch (NotFoundException | ConflictException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Impossible de valider le fournisseur {} via l'API Organization : {}", supplierId, e.getMessage());
            throw new ConflictException("Service de validation des organisations indisponible");
        }
    }

    public void validateRelation(long shopId, long supplierId) {
        String url = gatewayBaseUrl + "/api/organizations/internal/relations/supplier/" + supplierId;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("X-Internal-Token", internalSecret)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 && response.body().contains("\"ACTIVE\"")) {
                if (response.body().contains("\"shopId\":" + shopId)) {
                    return;
                }
            }
            throw new ConflictException("Aucune relation active entre le fournisseur " + supplierId
                    + " et la boutique " + shopId);
        } catch (ConflictException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Impossible de valider la relation {} <-> {} : {}", shopId, supplierId, e.getMessage());
            throw new ConflictException("Service de validation des organisations indisponible");
        }
    }

    public Optional<String> getOrganizationName(long organizationId) {
        String url = gatewayBaseUrl + "/api/organizations/internal/" + organizationId + "/status";
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("X-Internal-Token", internalSecret)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode node = objectMapper.readTree(response.body());
                if (node.has("name")) {
                    return Optional.of(node.get("name").asText());
                }
            }
        } catch (Exception e) {
            log.debug("Could not fetch organization name for {}: {}", organizationId, e.getMessage());
        }
        return Optional.empty();
    }

    public Optional<String> getUserName(long userId) {
        String url = gatewayBaseUrl + "/api/users/" + userId;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("X-Internal-Token", internalSecret)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode node = objectMapper.readTree(response.body());
                String firstName = node.has("firstName") ? node.get("firstName").asText() : "";
                String lastName = node.has("lastName") ? node.get("lastName").asText() : "";
                return Optional.of((firstName + " " + lastName).trim());
            }
        } catch (Exception e) {
            log.debug("Could not fetch user name for {}: {}", userId, e.getMessage());
        }
        return Optional.empty();
    }
}
