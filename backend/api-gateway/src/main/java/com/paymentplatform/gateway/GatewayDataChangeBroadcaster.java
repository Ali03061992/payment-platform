package com.paymentplatform.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Notifie le service notification après une mutation métier réussie : tous les
 * clients SSE revalident leur cache. L'appel est asynchrone et ne doit jamais
 * impacter la latence ni le résultat de la requête client.
 */
@Component
public class GatewayDataChangeBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(GatewayDataChangeBroadcaster.class);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();
    private final URI target;
    private final String internalSecret;

    public GatewayDataChangeBroadcaster(
            @Value("${NOTIFICATION_SERVICE_URL:localhost}") String notificationHost,
            @Value("${NOTIFICATION_SERVICE_PORT:8085}") String notificationPort,
            @Value("${app.internal-secret:local-internal-secret-change-me}") String internalSecret) {
        this.target = URI.create("http://" + notificationHost + ":" + notificationPort
                + "/api/internal/notifications/data-changed");
        this.internalSecret = internalSecret;
    }

    public CompletableFuture<HttpResponse<Void>> broadcast() {
        HttpRequest request = HttpRequest.newBuilder(target)
                .timeout(Duration.ofSeconds(2))
                .header("X-Internal-Token", internalSecret)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .whenComplete((response, error) -> {
                    if (error != null) {
                        log.debug("Data-change broadcast failed: {}", error.getMessage());
                    } else if (response.statusCode() >= 300) {
                        log.debug("Data-change broadcast rejected with status {}", response.statusCode());
                    }
                });
    }
}
