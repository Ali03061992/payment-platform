package com.paymentplatform.payment.application.service;

import com.paymentplatform.payment.application.dto.PaymentResponse;
import com.paymentplatform.payment.domain.model.PaymentStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.paymentplatform.payment.domain.model.PaymentStatus.CANCELLED;
import static com.paymentplatform.payment.domain.model.PaymentStatus.CONFIRMED;
import static com.paymentplatform.payment.domain.model.PaymentStatus.PENDING;
import static com.paymentplatform.payment.domain.model.PaymentStatus.REJECTED;
import static org.assertj.core.api.Assertions.assertThat;

class PaymentInvoicePdfServiceTest {

    private final PaymentInvoicePdfService service = new PaymentInvoicePdfService();

    private PaymentResponse payment(PaymentStatus status) {
        return new PaymentResponse(
                UUID.randomUUID(),
                "PAY-TEST-001",
                UUID.fromString("00000000-0000-0000-0000-000000000010"),
                "Boutique Test",
                UUID.fromString("00000000-0000-0000-0000-000000000020"),
                "Fournisseur Test",
                new BigDecimal("119.00"),
                "TND",
                status,
                "REJECTED".equals(status.name()) ? "Non conforme" : null,
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "Créateur",
                CONFIRMED.equals(status) ? "Admin" : null,
                REJECTED.equals(status) ? "Admin" : null,
                CANCELLED.equals(status) ? "Admin" : null,
                UUID.randomUUID(),
                null,
                1L,
                Instant.now(),
                Instant.now(),
                List.of()
        );
    }

    @Test
    void generateInvoicePdf_confirmedPayment_returnsPdfBytes() {
        byte[] pdf = service.generateInvoicePdf(payment(CONFIRMED));

        assertThat(pdf).isNotEmpty();
        assertThat(pdf[0]).isEqualTo((byte) '%');
        assertThat(pdf[1]).isEqualTo((byte) 'P');
        assertThat(pdf[2]).isEqualTo((byte) 'D');
        assertThat(pdf[3]).isEqualTo((byte) 'F');
    }

    @Test
    void generateInvoicePdf_pendingPayment_returnsPdfBytes() {
        byte[] pdf = service.generateInvoicePdf(payment(PENDING));

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
    }

    @Test
    void generateInvoicePdf_nullNames_returnsPdfBytes() {
        PaymentResponse base = payment(CONFIRMED);
        PaymentResponse withoutNames = new PaymentResponse(
                base.id(), base.reference(), base.shopId(), null,
                base.supplierId(), null, base.amount(), base.currency(),
                base.status(), base.rejectionReason(), base.createdBy(), null,
                null, null, null, base.orderId(), base.dueDate(),
                base.version(), base.createdAt(), base.updatedAt(), base.events());

        byte[] pdf = service.generateInvoicePdf(withoutNames);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
    }
}
