package com.paymentplatform.payment.infrastructure.csv;

import java.util.UUID;

import com.paymentplatform.payment.domain.model.Payment;
import com.paymentplatform.payment.domain.repository.PaymentRepository;
import com.paymentplatform.payment.infrastructure.http.OrganizationValidationClient;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class CsvExportService {

    private final PaymentRepository payments;
    private final OrganizationValidationClient orgClient;

    public CsvExportService(PaymentRepository payments, OrganizationValidationClient orgClient) {
        this.payments = payments;
        this.orgClient = orgClient;
    }

    public String generatePaymentsCsv(Instant from, Instant to, UUID supplierId, UUID shopId) {
        List<Payment> allPayments = payments.findAll();

        List<Payment> filtered = allPayments.stream()
                .filter(p -> p.createdAt().isAfter(from.minusMillis(1)) && p.createdAt().isBefore(to))
                .filter(p -> supplierId == null || p.supplierId() == supplierId)
                .filter(p -> shopId == null || p.shopId() == shopId)
                .toList();

        StringWriter sw = new StringWriter();
        sw.write("Reference,Shop,Supplier,Amount,Currency,Status,Created\n");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.of("UTC"));

        for (Payment p : filtered) {
            String shopName = orgClient.getOrganizationName(p.shopId()).orElse("Shop " + p.shopId());
            String supplierName = orgClient.getOrganizationName(p.supplierId()).orElse("Supplier " + p.supplierId());
            sw.write(String.format("%s,\"%s\",\"%s\",%s,%s,%s,%s\n",
                    p.reference().value(),
                    shopName.replace("\"", "\"\""),
                    supplierName.replace("\"", "\"\""),
                    p.money().amount().toPlainString(),
                    p.money().currency(),
                    p.status().name(),
                    formatter.format(p.createdAt())
            ));
        }

        return sw.toString();
    }
}
