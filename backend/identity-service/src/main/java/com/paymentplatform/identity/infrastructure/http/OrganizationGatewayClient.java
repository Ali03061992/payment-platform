package com.paymentplatform.identity.infrastructure.http;

import com.paymentplatform.identity.application.port.OrganizationStatus;
import com.paymentplatform.identity.application.port.OrganizationStatusPort;
import com.paymentplatform.shared.domain.exception.NotFoundException;
import com.paymentplatform.shared.domain.exception.UnprocessableEntityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client vers Organization Service (appel direct), avec cache court (10 s) du statut.
 * Périmètre : ne consulte jamais les tables d'un autre service.
 */
@Component
@Profile("!test")
public class OrganizationGatewayClient implements OrganizationStatusPort {

    private static final Logger log = LoggerFactory.getLogger(OrganizationGatewayClient.class);

    private record CacheEntry(OrganizationStatus status, Instant expiresAt) {
        boolean valid() {
            return expiresAt.isAfter(Instant.now());
        }
    }

    private static final long CACHE_TTL_MS = 10_000;

    private final RestClient restClient;
    private final String internalSecret;
    private final Map<UUID, CacheEntry> cache = new ConcurrentHashMap<>();

    public OrganizationGatewayClient(RestClient.Builder builder,
                                     @Value("${ORGANIZATION_SERVICE_URL:localhost}") String orgHost,
                                     @Value("${ORGANIZATION_SERVICE_PORT:8083}") String orgPort,
                                     @Value("${app.internal-secret}") String internalSecret) {
        this.restClient = builder.baseUrl("http://" + orgHost + ":" + orgPort).build();
        this.internalSecret = internalSecret;
    }

    @Override
    public OrganizationStatus getOrganizationStatus(UUID organizationId) {
        CacheEntry cached = cache.get(organizationId);
        if (cached != null && cached.valid()) {
            return cached.status();
        }
        OrganizationStatus status = fetch(organizationId);
        cache.put(organizationId, new CacheEntry(status, Instant.now().plusMillis(CACHE_TTL_MS)));
        return status;
    }

    private OrganizationStatus fetch(UUID organizationId) {
        try {
            OrganizationStatus status = restClient.get()
                    .uri("/api/organizations/internal/{id}/status", organizationId)
                    .header("X-Internal-Token", internalSecret)
                    .retrieve()
                    .body(OrganizationStatus.class);
            if (status == null) {
                throw new NotFoundException("Organisation introuvable : " + organizationId);
            }
            return status;
        } catch (RestClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new NotFoundException("Organisation introuvable : " + organizationId);
            }
            log.error("Erreur de consultation de l'organisation {}", organizationId, e);
            throw new UnprocessableEntityException("Organisation indisponible : " + organizationId);
        }
    }
}