package com.paymentplatform.organization.infrastructure.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.UUID;

@Component
public class PaymentClient {

    private static final Logger log = LoggerFactory.getLogger(PaymentClient.class);

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${PAYMENT_SERVICE_URL:localhost}")
    private String paymentUrl;

    @Value("${PAYMENT_SERVICE_PORT:8084}")
    private String paymentPort;

    public void createAutoPayment(UUID shopId, UUID supplierId, String currency, UUID createdBy) {
        try {
            String targetUri = "http://" + paymentUrl + ":" + paymentPort + "/api/payments";
            String json = """
                    {
                      "shopId": "%s",
                      "supplierId": "%s",
                      "amount": 0,
                      "currency": "%s"
                    }
                    """.formatted(shopId, supplierId, currency != null ? currency : "TND");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(targetUri))
                    .header("Content-Type", "application/json")
                    .header("X-System-Admin", "true")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Auto-payment created for order: shopId={}, supplierId={}", shopId, supplierId);
            } else {
                log.warn("Failed to auto-create payment: status={}, body={}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.error("Failed to auto-create payment for ASAP order", e);
        }
    }
}
