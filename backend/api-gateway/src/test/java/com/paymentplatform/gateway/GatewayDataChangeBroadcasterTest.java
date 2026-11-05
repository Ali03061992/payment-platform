package com.paymentplatform.gateway;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.http.HttpResponse;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Vérifie que le signal data-changed part bien en POST interne authentifié,
 * sans jamais bloquer la réponse client (future complétée en arrière-plan).
 */
class GatewayDataChangeBroadcasterTest {

    private HttpServer server;
    private final AtomicReference<String> method = new AtomicReference<>();
    private final AtomicReference<String> token = new AtomicReference<>();
    private final AtomicReference<String> path = new AtomicReference<>();

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/api/internal/notifications/data-changed", exchange -> {
            method.set(exchange.getRequestMethod());
            token.set(exchange.getRequestHeaders().getFirst("X-Internal-Token"));
            path.set(exchange.getRequestURI().getPath());
            exchange.sendResponseHeaders(202, -1);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void broadcast_postsInternalTokenAndPath() {
        GatewayDataChangeBroadcaster broadcaster = new GatewayDataChangeBroadcaster(
                "localhost", String.valueOf(server.getAddress().getPort()), "secret-test");

        HttpResponse<Void> response = broadcaster.broadcast().join();

        assertThat(response.statusCode()).isEqualTo(202);
        assertThat(method.get()).isEqualTo("POST");
        assertThat(token.get()).isEqualTo("secret-test");
        assertThat(path.get()).isEqualTo("/api/internal/notifications/data-changed");
    }
}
