package com.paymentplatform.organization.infrastructure.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;

@Component
public class PaymentClient {

    private static final Logger log = LoggerFactory.getLogger(PaymentClient.class);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    @Value("${PAYMENT_SERVICE_URL:localhost}")
    private String paymentUrl;

    @Value("${PAYMENT_SERVICE_PORT:8084}")
    private String paymentPort;

    @Value("${app.internal-secret}")
    private String internalSecret;

    /**
     * Crée le paiement auto d'une commande ASAP via l'endpoint interne
     * {@code POST /api/internal/payments/auto} (authentifié par
     * X-Internal-Token — l'ancien appel anonyme recevait 401).
     * L'idempotence est portée par la commande (clé {@code asap-<orderId>},
     * cf. B1) : livraison + accept-asap ne créent qu'un seul paiement.
     */
    public void createAutoPayment(UUID shopId, UUID supplierId, String currency, UUID createdBy,
                                   BigDecimal amount, UUID orderId) {
        // 2 tentatives max (l'idempotence côté paiement rend le rejeu sûr).
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                String targetUri = "http://" + paymentUrl + ":" + paymentPort + "/api/internal/payments/auto";
                String json = """
                        {
                          "shopId": "%s",
                          "supplierId": "%s",
                          "amount": %s,
                          "currency": "%s",
                          "orderId": %s,
                          "dueDate": null
                        }
                        """.formatted(shopId, supplierId,
                        amount != null ? amount.toPlainString() : "0",
                        currency != null ? currency : "TND",
                        orderId != null ? "\"" + orderId + "\"" : "null");

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(targetUri))
                        .header("Content-Type", "application/json")
                        .header("X-Internal-Token", internalSecret)
                        .header("X-Actor-User-Id", createdBy != null ? createdBy.toString() : "")
                        .timeout(Duration.ofSeconds(5))
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    log.info("Auto-payment created for order {}: shopId={}, supplierId={}, amount={}",
                            orderId, shopId, supplierId, amount);
                    return;
                }
                if (response.statusCode() >= 500 && attempt < 2) {
                    log.warn("Auto-payment retry after HTTP {} for order {}", response.statusCode(), orderId);
                    continue;
                }
                log.warn("Failed to auto-create payment: status={}, body={}", response.statusCode(), response.body());
                return;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Failed to auto-create payment for ASAP order", e);
                return;
            } catch (Exception e) {
                if (attempt < 2) {
                    log.warn("Auto-payment retry after error for order {}: {}", orderId, e.getMessage());
                    continue;
                }
                log.error("Failed to auto-create payment for ASAP order", e);
            }
        }
    }
}
