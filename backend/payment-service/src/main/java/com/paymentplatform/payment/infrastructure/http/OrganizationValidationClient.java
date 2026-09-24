package com.paymentplatform.payment.infrastructure.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentplatform.shared.domain.exception.ConflictException;
import com.paymentplatform.shared.domain.exception.NotFoundException;
import com.paymentplatform.shared.domain.exception.ServiceUnavailableException;
import com.paymentplatform.shared.domain.security.InternalSecretValidator;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * M2 : validation inter-services durcie.
 *
 * <ul>
 *   <li>Timeouts : connexion 2 s, requête 5 s (plus aucun appel bloquant).</li>
 *   <li>Retry : 3 tentatives, backoff 200/400 ms, uniquement sur IO/timeout/5xx
 *   (jamais sur 4xx métier).</li>
 *   <li>Circuit-breaker par aval (5 échecs consécutifs =&gt; ouvert 30 s) :
 *   échec rapide 503 au lieu d'attendre les timeouts.</li>
 *   <li>Parsing JSON typé (champs {@code type}/{@code status}/{@code shopId}) —
 *   plus de {@code body.contains(...)}.</li>
 *   <li>409 métier (règle violée) vs 503 infra (aval en panne) distingués.</li>
 * </ul>
 */
@Component
public class OrganizationValidationClient {

    private static final Logger log = LoggerFactory.getLogger(OrganizationValidationClient.class);

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);
    private static final int MAX_ATTEMPTS = 3;
    private static final long[] BACKOFF_MS = {200, 400};

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConcurrentHashMap<String, SimpleCircuitBreaker> breakers = new ConcurrentHashMap<>();

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
        this.internalSecret = InternalSecretValidator.requireValid(internalSecret, environment);
    }

    public void validateShop(UUID shopId) {
        JsonNode status = fetchOrganizationStatus(shopId);
        if (!"SHOP".equals(status.path("type").asText())) {
            throw new ConflictException("L'organisation " + shopId + " n'est pas une boutique");
        }
        assertActive(status, "La boutique " + shopId + " est désactivée");
    }

    public void validateSupplier(UUID supplierId) {
        JsonNode status = fetchOrganizationStatus(supplierId);
        if (!"SUPPLIER".equals(status.path("type").asText())) {
            throw new ConflictException("L'organisation " + supplierId + " n'est pas un fournisseur");
        }
        assertActive(status, "Le fournisseur " + supplierId + " est désactivé");
    }

    private JsonNode fetchOrganizationStatus(UUID orgId) {
        String url = "http://" + organizationServiceUrl + ":" + organizationServicePort
                + "/api/organizations/internal/" + orgId + "/status";
        HttpResponse<String> response = send("organization", url);
        if (response.statusCode() == 404) {
            throw new NotFoundException("Organisation introuvable : " + orgId);
        }
        if (response.statusCode() != 200) {
            throw new ConflictException("Erreur validation organisation : HTTP " + response.statusCode());
        }
        return parseObject(response.body(), "statut d'organisation");
    }

    private static void assertActive(JsonNode status, String disabledMessage) {
        if (!"ACTIVE".equals(status.path("status").asText())) {
            throw new ConflictException(disabledMessage);
        }
    }

    public void validateRelation(UUID shopId, UUID supplierId) {
        String url = "http://" + organizationServiceUrl + ":" + organizationServicePort
                + "/api/organizations/internal/relations/supplier/" + supplierId;
        HttpResponse<String> response = send("organization", url);
        if (response.statusCode() != 200) {
            throw new ServiceUnavailableException(
                    "Service de validation des organisations indisponible (HTTP " + response.statusCode() + ")");
        }
        JsonNode array;
        try {
            array = objectMapper.readTree(response.body());
        } catch (Exception e) {
            throw new ServiceUnavailableException("Réponse inattendu du service organisations", e);
        }
        if (array.isArray()) {
            for (JsonNode relation : array) {
                boolean sameShop = shopId.toString().equals(relation.path("shopId").asText());
                boolean active = "ACTIVE".equals(relation.path("status").asText());
                if (sameShop && active) {
                    return;
                }
            }
        }
        throw new ConflictException("Aucune relation active entre le fournisseur " + supplierId
                + " et la boutique " + shopId);
    }

    @Cacheable(value = "organizations", key = "#organizationId")
    public Optional<String> getOrganizationName(UUID organizationId) {
        String url = "http://" + organizationServiceUrl + ":" + organizationServicePort
                + "/api/organizations/internal/" + organizationId + "/status";
        try {
            HttpResponse<String> response = send("organization", url);
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
            HttpResponse<String> response = send("identity", url);
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

    private JsonNode parseObject(String body, String what) {
        try {
            JsonNode node = objectMapper.readTree(body);
            if (!node.isObject()) {
                throw new ServiceUnavailableException("Réponse inattendue du service (" + what + ")");
            }
            return node;
        } catch (ServiceUnavailableException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceUnavailableException("Réponse inattendue du service (" + what + ")", e);
        }
    }

    /**
     * Envoi avec circuit-breaker + retry. Ne propage que des réponses HTTP ;
     * toute panne réseau/timeout/5xx devient {@link ServiceUnavailableException}
     * (503), jamais 409.
     */
    private HttpResponse<String> send(String downstream, String url) {
        SimpleCircuitBreaker breaker = breakers.computeIfAbsent(downstream, k -> new SimpleCircuitBreaker());
        if (!breaker.allowRequest()) {
            throw new ServiceUnavailableException(
                    "Service " + downstream + " temporairement indisponible (circuit ouvert)");
        }
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("X-Internal-Token", internalSecret)
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();
        for (int attempt = 1; ; attempt++) {
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                int code = response.statusCode();
                if (code == 401 || code == 403) {
                    breaker.recordFailure();
                    throw new ServiceUnavailableException(
                            "Service " + downstream + " indisponible (HTTP " + code + " — secret interne ?)");
                }
                if (code >= 500 && attempt < MAX_ATTEMPTS) {
                    breaker.recordFailure();
                    backoff(attempt);
                    continue;
                }
                if (code >= 500) {
                    breaker.recordFailure();
                    throw new ServiceUnavailableException(
                            "Service " + downstream + " indisponible (HTTP " + code + ")");
                }
                breaker.recordSuccess();
                return response;
            } catch (ServiceUnavailableException e) {
                throw e;
            } catch (HttpTimeoutException e) {
                breaker.recordFailure();
                if (attempt < MAX_ATTEMPTS) {
                    backoff(attempt);
                    continue;
                }
                throw new ServiceUnavailableException(
                        "Service " + downstream + " indisponible (timeout " + REQUEST_TIMEOUT.getSeconds() + "s)", e);
            } catch (IOException e) {
                breaker.recordFailure();
                if (attempt < MAX_ATTEMPTS) {
                    backoff(attempt);
                    continue;
                }
                throw new ServiceUnavailableException("Service " + downstream + " injoignable", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ServiceUnavailableException("Validation interrompue", e);
            }
        }
    }

    private static void backoff(int attempt) {
        try {
            Thread.sleep(BACKOFF_MS[Math.min(attempt - 1, BACKOFF_MS.length - 1)]);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceUnavailableException("Validation interrompue", e);
        }
    }

    /**
     * Disjoncteur minimaliste : CLOSED → 5 échecs consécutifs → OPEN (30 s) →
     * une sonde (HALF_OPEN) → CLOSED ou OPEN.
     */
    static class SimpleCircuitBreaker {
        private static final int FAILURE_THRESHOLD = 5;
        private static final long OPEN_DURATION_MS = 30_000;

        private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
        private final AtomicLong openedAt = new AtomicLong(0);
        private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);

        private enum State { CLOSED, OPEN, HALF_OPEN }

        boolean allowRequest() {
            if (state.get() == State.OPEN) {
                if (System.currentTimeMillis() - openedAt.get() >= OPEN_DURATION_MS) {
                    if (state.compareAndSet(State.OPEN, State.HALF_OPEN)) {
                        return true;
                    }
                    return state.get() != State.OPEN;
                }
                return false;
            }
            return true;
        }

        void recordSuccess() {
            consecutiveFailures.set(0);
            state.set(State.CLOSED);
        }

        void recordFailure() {
            if (state.get() == State.HALF_OPEN) {
                state.set(State.OPEN);
                openedAt.set(System.currentTimeMillis());
                return;
            }
            if (consecutiveFailures.incrementAndGet() >= FAILURE_THRESHOLD) {
                state.set(State.OPEN);
                openedAt.set(System.currentTimeMillis());
            }
        }
    }
}
