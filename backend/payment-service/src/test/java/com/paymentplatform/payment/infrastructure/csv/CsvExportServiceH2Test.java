package com.paymentplatform.payment.infrastructure.csv;

import com.paymentplatform.payment.application.dto.CreatePaymentRequest;
import com.paymentplatform.payment.application.usecase.CreatePaymentUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(com.paymentplatform.payment.infrastructure.http.TestOrganizationValidationConfig.class)
class CsvExportServiceH2Test {

    @Autowired private CsvExportService csvExportService;
    @Autowired private CreatePaymentUseCase createPayment;

    @Test
    void generatePaymentsCsv_withData_returnsCsv() {
        createPayment.execute(new CreatePaymentRequest(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), new BigDecimal("100"), "TND"), UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000001"));
        createPayment.execute(new CreatePaymentRequest(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000003"), new BigDecimal("200"), "EUR"), UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000001"));

        Instant from = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant to = Instant.now().plus(1, ChronoUnit.DAYS);

        String csv = csvExportService.generatePaymentsCsv(from, to, null, null);
        assertThat(csv).contains("Reference,Shop,Supplier");
        assertThat(csv).contains("PAY-");
    }

    @Test
    void generatePaymentsCsv_noData_returnsHeaderOnly() {
        Instant from = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant to = Instant.now().plus(1, ChronoUnit.DAYS);

        String csv = csvExportService.generatePaymentsCsv(from, to, null, null);
        assertThat(csv).startsWith("Reference,Shop,Supplier");
    }

    @Test
    void generatePaymentsCsv_filterBySupplier() {
        createPayment.execute(new CreatePaymentRequest(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), new BigDecimal("100"), "TND"), UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000001"));
        createPayment.execute(new CreatePaymentRequest(UUID.fromString("00000000-0000-0000-0000-000000000003"), UUID.fromString("00000000-0000-0000-0000-000000000004"), new BigDecimal("200"), "TND"), UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000002"));

        Instant from = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant to = Instant.now().plus(1, ChronoUnit.DAYS);

        String csv = csvExportService.generatePaymentsCsv(from, to, UUID.fromString("00000000-0000-0000-0000-000000000002"), null);
        String[] lines = csv.split("\n");
        assertThat(lines.length).isEqualTo(2); // header + 1 data line
    }
}
