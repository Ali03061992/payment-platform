package com.paymentplatform.organization.infrastructure.http;

import com.paymentplatform.organization.application.dto.SupplierFinancialReportResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

@Component
public class PaymentSummaryClient {

    private static final Logger log = LoggerFactory.getLogger(PaymentSummaryClient.class);
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${PAYMENT_SERVICE_URL:localhost}")
    private String paymentUrl;

    @Value("${PAYMENT_SERVICE_PORT:8084}")
    private String paymentPort;

    public SupplierFinancialReportResponse.PaymentSummary getSupplierPaymentSummary(UUID supplierId) {
        try {
            String targetUri = "http://" + paymentUrl + ":" + paymentPort
                    + "/api/payments/supplier-summary?supplierId=" + supplierId;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(targetUri))
                    .header("X-System-Admin", "true")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return parseSummary(response.body());
            } else {
                log.warn("Failed to fetch payment summary: status={}, body={}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.error("Failed to fetch payment summary for supplier {}", supplierId, e);
        }
        return new SupplierFinancialReportResponse.PaymentSummary(
                BigDecimal.ZERO, 0, BigDecimal.ZERO, 0, BigDecimal.ZERO, 0);
    }

    private SupplierFinancialReportResponse.PaymentSummary parseSummary(String json) {
        try {
            BigDecimal confirmedTotal = extractBigDecimal(json, "confirmedTotal");
            long confirmedCount = extractLong(json, "confirmedCount");
            BigDecimal pendingTotal = extractBigDecimal(json, "pendingTotal");
            long pendingCount = extractLong(json, "pendingCount");
            BigDecimal rejectedTotal = extractBigDecimal(json, "rejectedTotal");
            long rejectedCount = extractLong(json, "rejectedCount");
            return new SupplierFinancialReportResponse.PaymentSummary(
                    confirmedTotal, confirmedCount, pendingTotal, pendingCount, rejectedTotal, rejectedCount);
        } catch (Exception e) {
            log.error("Failed to parse payment summary JSON", e);
            return new SupplierFinancialReportResponse.PaymentSummary(
                    BigDecimal.ZERO, 0, BigDecimal.ZERO, 0, BigDecimal.ZERO, 0);
        }
    }

    private BigDecimal extractBigDecimal(String json, String field) {
        int idx = json.indexOf("\"" + field + "\"");
        if (idx < 0) return BigDecimal.ZERO;
        int colonIdx = json.indexOf(':', idx);
        int endIdx = colonIdx + 1;
        while (endIdx < json.length() && (Character.isDigit(json.charAt(endIdx)) || json.charAt(endIdx) == '.' || json.charAt(endIdx) == '-')) {
            endIdx++;
        }
        String val = json.substring(colonIdx + 1, endIdx).trim();
        return val.isEmpty() || "null".equals(val) ? BigDecimal.ZERO : new BigDecimal(val);
    }

    private long extractLong(String json, String field) {
        int idx = json.indexOf("\"" + field + "\"");
        if (idx < 0) return 0;
        int colonIdx = json.indexOf(':', idx);
        int endIdx = colonIdx + 1;
        while (endIdx < json.length() && Character.isDigit(json.charAt(endIdx))) {
            endIdx++;
        }
        String val = json.substring(colonIdx + 1, endIdx).trim();
        return val.isEmpty() || "null".equals(val) ? 0 : Long.parseLong(val);
    }
}
