package com.paymentplatform.payment.infrastructure.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentplatform.shared.domain.exception.ConflictException;
import com.paymentplatform.shared.domain.exception.NotFoundException;
import com.paymentplatform.shared.domain.security.InternalSecretValidator;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.UUID;

@Component
public class OrganizationValidationClient {

    private static final Logger log = LoggerFactory.getLogger(OrganizationValidationClient.class);
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.organization-service.url:localhost}")
    private String organizationServiceUrl;

    @Value("${app.organization-service.port:8083}")
    private int organizationServicePort;

    @Value("${app.identity-service.url:identity-service}")
    private String identityServiceUrl;

    @Value("${app.identity-service.port:8082}")
    private int identityServicePort;

    @Value("${app.internal-secret}")
    private String internalSecret;

    @Autowired
    private Environment environment;

    @PostConstruct
    void validateInternalSecret() {
        // B3 : échec au boot si absent ; refus des défauts connus sous profil prod.
        this.internalSecret = InternalSecretValidator.requireValid(internalSecret, environment);
    }

    public void validateShop(UUID shopId) {
        String url = "http://" + organizationServiceUrl + ":" + organizationServicePort + "/api/organizations/internal/" + shopId + "/status";
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

    public void validateSupplier(UUID supplierId) {
        String url = "http://" + organizationServiceUrl + ":" + organizationServicePort + "/api/organizations/internal/" + supplierId + "/status";
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

    public void validateRelation(UUID shopId, UUID supplierId) {
        String url = "http://" + organizationServiceUrl + ":" + organizationServicePort + "/api/organizations/internal/relations/supplier/" + supplierId;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("X-Internal-Token", internalSecret)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 && response.body().contains("\"ACTIVE\"")) {
                if (response.body().contains("\"shopId\":\"" + shopId + "\"")) {
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

    @Cacheable(value = "organizations", key = "#organizationId")
    public Optional<String> getOrganizationName(UUID organizationId) {
        String url = "http://" + organizationServiceUrl + ":" + organizationServicePort + "/api/organizations/internal/" + organizationId + "/status";
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

    @Cacheable(value = "users", key = "#userId")
    public Optional<String> getUserName(UUID userId) {
        String url = "http://" + identityServiceUrl + ":" + identityServicePort + "/api/internal/users/" + userId;
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
                String username = node.has("username") ? node.get("username").asText() : "";
                if (!firstName.isEmpty() && !lastName.isEmpty()) {
                    return Optional.of(firstName + " " + lastName);
                }
                return Optional.of(username);
            }
        } catch (Exception e) {
            log.debug("Could not fetch user name for {}: {}", userId, e.getMessage());
        }
        return Optional.empty();
    }
}
