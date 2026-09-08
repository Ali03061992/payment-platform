package com.paymentplatform.payment.infrastructure.csv;

import com.paymentplatform.payment.application.dto.CreatePaymentRequest;
import com.paymentplatform.payment.application.usecase.CreatePaymentUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CsvExportServiceH2Test {

    @Autowired private CsvExportService csvExportService;
    @Autowired private CreatePaymentUseCase createPayment;

    @Test
    void generatePaymentsCsv_withData_returnsCsv() {
        createPayment.execute(new CreatePaymentRequest(1L, 2L, new BigDecimal("100"), "TND"), 10L, 1L);
        createPayment.execute(new CreatePaymentRequest(1L, 3L, new BigDecimal("200"), "EUR"), 10L, 1L);

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
        createPayment.execute(new CreatePaymentRequest(1L, 2L, new BigDecimal("100"), "TND"), 10L, 1L);
        createPayment.execute(new CreatePaymentRequest(3L, 4L, new BigDecimal("200"), "TND"), 10L, 2L);

        Instant from = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant to = Instant.now().plus(1, ChronoUnit.DAYS);

        String csv = csvExportService.generatePaymentsCsv(from, to, 2L, null);
        String[] lines = csv.split("\n");
        assertThat(lines.length).isEqualTo(2); // header + 1 data line
    }
}
