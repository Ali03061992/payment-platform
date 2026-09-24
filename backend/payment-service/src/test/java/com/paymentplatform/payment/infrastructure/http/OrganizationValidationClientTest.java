package com.paymentplatform.payment.infrastructure.http;

import com.paymentplatform.shared.domain.exception.ConflictException;
import com.paymentplatform.shared.domain.exception.NotFoundException;
import com.paymentplatform.shared.domain.exception.ServiceUnavailableException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * M2 : timeouts, retry, circuit-breaker, parsing typé et 409 vs 503.
 * Test unitaire pur (pas de contexte Spring) avec stub HTTP du JDK.
 */
class OrganizationValidationClientTest {

    private HttpServer stub;
    private int stubPort;
    private final Map<String, StubResponse> routes = new ConcurrentHashMap<>();
    private final AtomicInteger hits = new AtomicInteger(0);

    private record StubResponse(int status, String body, long delayMs) {
    }

    private OrganizationValidationClient client;

    @BeforeEach
    void setUp() throws IOException {
        stub = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        stubPort = stub.getAddress().getPort();
        stub.createContext("/", exchange -> {
            hits.incrementAndGet();
            String path = exchange.getRequestURI().getPath();
            StubResponse response = routes.getOrDefault(path,
                    new StubResponse(404, "{\"message\":\"not found\"}", 0));
            if (response.delayMs() > 0) {
                try {
                    Thread.sleep(response.delayMs());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            byte[] body = response.body().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(response.status(), body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        stub.start();

        client = new OrganizationValidationClient();
        ReflectionTestUtils.setField(client, "organizationServiceUrl", "127.0.0.1");
        ReflectionTestUtils.setField(client, "organizationServicePort", stubPort);
        ReflectionTestUtils.setField(client, "identityServiceUrl", "127.0.0.1");
        ReflectionTestUtils.setField(client, "identityServicePort", stubPort);
        ReflectionTestUtils.setField(client, "internalSecret", "s3cr3t");
    }

    @AfterEach
    void tearDown() {
        stub.stop(0);
        routes.clear();
        hits.set(0);
    }

    private static String statusBody(String type, String status) {
        return "{\"id\":\"00000000-0000-0000-0000-000000000001\",\"name\":\"X\","
                + "\"type\":\"" + type + "\",\"status\":\"" + status + "\"}";
    }

    @Test
    void validateShop_ok_noThrow() {
        UUID shop = UUID.randomUUID();
        routes.put("/api/organizations/internal/" + shop + "/status",
                new StubResponse(200, statusBody("SHOP", "ACTIVE"), 0));

        client.validateShop(shop);

        assertThat(hits.get()).isEqualTo(1);
    }

    @Test
    void validateShop_wrongType_throwsConflict() {
        UUID org = UUID.randomUUID();
        routes.put("/api/organizations/internal/" + org + "/status",
                new StubResponse(200, statusBody("SUPPLIER", "ACTIVE"), 0));

        assertThatThrownBy(() -> client.validateShop(org))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("boutique");
        // Pas de retry sur erreur métier.
        assertThat(hits.get()).isEqualTo(1);
    }

    @Test
    void validateShop_disabled_throwsConflict() {
        UUID org = UUID.randomUUID();
        routes.put("/api/organizations/internal/" + org + "/status",
                new StubResponse(200, statusBody("SHOP", "DISABLED"), 0));

        assertThatThrownBy(() -> client.validateShop(org))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("désactivée");
    }

    @Test
    void validateShop_notFound_throwsNotFound() {
        assertThatThrownBy(() -> client.validateShop(UUID.randomUUID()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void validateShop_malformedBody_throwsServiceUnavailable() {
        UUID org = UUID.randomUUID();
        routes.put("/api/organizations/internal/" + org + "/status",
                new StubResponse(200, "ceci n'est pas du json {{{", 0));

        assertThatThrownBy(() -> client.validateShop(org))
                .isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    void validateRelation_matchingActiveRelation_ok() {
        UUID supplier = UUID.randomUUID();
        UUID shop = UUID.randomUUID();
        routes.put("/api/organizations/internal/relations/supplier/" + supplier,
                new StubResponse(200,
                        "[{\"id\":\"1\",\"supplierId\":\"" + supplier + "\",\"shopId\":\"" + shop + "\",\"status\":\"ACTIVE\"}]",
                        0));

        client.validateRelation(shop, supplier);
    }

    @Test
    void validateRelation_noMatch_throwsConflictNot503() {
        UUID supplier = UUID.randomUUID();
        UUID shop = UUID.randomUUID();
        routes.put("/api/organizations/internal/relations/supplier/" + supplier,
                new StubResponse(200,
                        "[{\"id\":\"1\",\"supplierId\":\"" + supplier + "\",\"shopId\":\"" + UUID.randomUUID() + "\",\"status\":\"ACTIVE\"}]",
                        0));

        // Règle métier violée => 409, pas 503.
        assertThatThrownBy(() -> client.validateRelation(shop, supplier))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void validateRelation_inactiveRelation_throwsConflict() {
        UUID supplier = UUID.randomUUID();
        UUID shop = UUID.randomUUID();
        routes.put("/api/organizations/internal/relations/supplier/" + supplier,
                new StubResponse(200,
                        "[{\"id\":\"1\",\"supplierId\":\"" + supplier + "\",\"shopId\":\"" + shop + "\",\"status\":\"INACTIVE\"}]",
                        0));

        assertThatThrownBy(() -> client.validateRelation(shop, supplier))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void retry_failsOnceThenSucceeds() {
        UUID shop = UUID.randomUUID();
        String path = "/api/organizations/internal/" + shop + "/status";
        AtomicInteger calls = new AtomicInteger(0);
        stub.removeContext("/");
        stub.createContext("/", exchange -> {
            hits.incrementAndGet();
            int n = calls.incrementAndGet();
            String body = n == 1 ? "{\"message\":\"boom\"}" : statusBody("SHOP", "ACTIVE");
            int code = n == 1 ? 500 : 200;
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            try {
                exchange.sendResponseHeaders(code, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        routes.put(path, new StubResponse(200, statusBody("SHOP", "ACTIVE"), 0));

        // Le stub custom ignore `routes` : il échoue une fois (500) puis réussit.
        client.validateShop(shop);

        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    void slowDownstream_timesOutInsteadOfHanging() {
        UUID org = UUID.randomUUID();
        routes.put("/api/organizations/internal/" + org + "/status",
                new StubResponse(200, statusBody("SHOP", "ACTIVE"), 7_000));

        long start = System.currentTimeMillis();
        assertThatThrownBy(() -> client.validateShop(org))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("timeout");
        long elapsed = System.currentTimeMillis() - start;
        // 3 tentatives x 5 s + backoff : borné (~16 s), jamais bloquant.
        assertThat(elapsed).isLessThan(30_000);
    }

    @Test
    void getOrganizationName_serverError_returnsEmptyWithoutThrowing() {
        UUID org = UUID.randomUUID();
        routes.put("/api/organizations/internal/" + org + "/status",
                new StubResponse(500, "{\"message\":\"boom\"}", 0));

        assertThat(client.getOrganizationName(org)).isEmpty();
    }

    @Test
    void circuitBreaker_opensAfterRepeatedFailures() {
        // Port fermé : connexion refusée immédiatement.
        ReflectionTestUtils.setField(client, "organizationServicePort", 9);

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> client.validateShop(UUID.randomUUID()))
                    .isInstanceOf(ServiceUnavailableException.class);
        }
        // 6e appel : circuit ouvert => échec immédiat, sans attente réseau.
        long start = System.currentTimeMillis();
        assertThatThrownBy(() -> client.validateShop(UUID.randomUUID()))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("circuit ouvert");
        assertThat(System.currentTimeMillis() - start).isLessThan(2000);
    }
}
