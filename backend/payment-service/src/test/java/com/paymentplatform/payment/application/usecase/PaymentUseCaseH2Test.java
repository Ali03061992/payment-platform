package com.paymentplatform.payment.application.usecase;

import java.util.UUID;

import com.paymentplatform.payment.application.dto.CreatePaymentRequest;
import com.paymentplatform.payment.application.dto.RejectPaymentRequest;
import com.paymentplatform.payment.domain.model.PaymentStatus;
import com.paymentplatform.payment.domain.repository.PaymentRepository;
import com.paymentplatform.shared.domain.exception.DomainException;
import com.paymentplatform.shared.domain.exception.ForbiddenException;
import com.paymentplatform.shared.domain.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PaymentUseCaseH2Test {

    @Autowired private CreatePaymentUseCase createPayment;
    @Autowired private ConfirmPaymentUseCase confirmPayment;
    @Autowired private RejectPaymentUseCase rejectPayment;
    @Autowired private CancelPaymentUseCase cancelPayment;
    @Autowired private GetPaymentUseCase getPayment;
    @Autowired private ListPaymentsUseCase listPayments;
    @Autowired private PaymentRepository payments;

    @Test
    void createPayment_validRequest_createsPayment() {
        var request = new CreatePaymentRequest(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), new BigDecimal("150.50"), "TND");
        var response = createPayment.execute(request, UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000001"));

        assertThat(response.id()).isNotNull();
        assertThat(response.reference()).startsWith("PAY-");
        assertThat(response.shopId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        assertThat(response.supplierId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("150.50"));
        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.createdBy()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000010"));
    }

    @Test
    void confirmPayment_pendingToConfirmed() {
        var created = createPayment.execute(
                new CreatePaymentRequest(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), new BigDecimal("100"), "TND"), UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000001"));
        var confirmed = confirmPayment.execute(created.id(), UUID.fromString("00000000-0000-0000-0000-000000000020"), UUID.fromString("00000000-0000-0000-0000-000000000002"));
        assertThat(confirmed.status()).isEqualTo("CONFIRMED");
    }

    @Test
    void confirmPayment_notFound_throws() {
        assertThatThrownBy(() -> confirmPayment.execute(UUID.fromString("00000000-0000-0000-0000-000000000999"), UUID.fromString("00000000-0000-0000-0000-000000000020"), UUID.fromString("00000000-0000-0000-0000-000000000002")))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void confirmPayment_wrongSupplier_throwsForbidden() {
        var created = createPayment.execute(
                new CreatePaymentRequest(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), new BigDecimal("100"), "TND"), UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000001"));
        assertThatThrownBy(() -> confirmPayment.execute(created.id(), UUID.fromString("00000000-0000-0000-0000-000000000020"), UUID.fromString("00000000-0000-0000-0000-000000000099")))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void confirmPayment_nullOrgId_succeeds() {
        var created = createPayment.execute(
                new CreatePaymentRequest(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), new BigDecimal("100"), "TND"), UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000001"));
        var confirmed = confirmPayment.execute(created.id(), UUID.fromString("00000000-0000-0000-0000-000000000020"), null);
        assertThat(confirmed.status()).isEqualTo("CONFIRMED");
    }

    @Test
    void rejectPayment_pendingToRejected() {
        var created = createPayment.execute(
                new CreatePaymentRequest(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), new BigDecimal("200"), "EUR"), UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000001"));
        var rejected = rejectPayment.execute(created.id(),
                new RejectPaymentRequest("Montant incorrect"), UUID.fromString("00000000-0000-0000-0000-000000000020"), UUID.fromString("00000000-0000-0000-0000-000000000002"));
        assertThat(rejected.status()).isEqualTo("REJECTED");
        assertThat(rejected.rejectionReason()).isEqualTo("Montant incorrect");
    }

    @Test
    void rejectPayment_wrongSupplier_throwsForbidden() {
        var created = createPayment.execute(
                new CreatePaymentRequest(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), new BigDecimal("200"), "EUR"), UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000001"));
        assertThatThrownBy(() -> rejectPayment.execute(created.id(),
                new RejectPaymentRequest("reason"), UUID.fromString("00000000-0000-0000-0000-000000000020"), UUID.fromString("00000000-0000-0000-0000-000000000099")))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void cancelPayment_pendingToCancelled() {
        var created = createPayment.execute(
                new CreatePaymentRequest(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), new BigDecimal("50"), "USD"), UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000001"));
        var cancelled = cancelPayment.execute(created.id(), UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000001"));
        assertThat(cancelled.status()).isEqualTo("CANCELLED");
    }

    @Test
    void cancelPayment_bySupplier_succeeds() {
        var created = createPayment.execute(
                new CreatePaymentRequest(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), new BigDecimal("50"), "USD"), UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000001"));
        var cancelled = cancelPayment.execute(created.id(), UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000002"));
        assertThat(cancelled.status()).isEqualTo("CANCELLED");
    }

    @Test
    void cancelPayment_wrongOrg_throwsForbidden() {
        var created = createPayment.execute(
                new CreatePaymentRequest(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), new BigDecimal("50"), "USD"), UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000001"));
        assertThatThrownBy(() -> cancelPayment.execute(created.id(), UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000099")))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void cancelPayment_nullOrgId_succeeds() {
        var created = createPayment.execute(
                new CreatePaymentRequest(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), new BigDecimal("50"), "USD"), UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000001"));
        var cancelled = cancelPayment.execute(created.id(), UUID.fromString("00000000-0000-0000-0000-000000000010"), null);
        assertThat(cancelled.status()).isEqualTo("CANCELLED");
    }

    @Test
    void confirmAlreadyConfirmed_throwsDomainException() {
        var created = createPayment.execute(
                new CreatePaymentRequest(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), new BigDecimal("100"), "TND"), UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000001"));
        confirmPayment.execute(created.id(), UUID.fromString("00000000-0000-0000-0000-000000000020"), UUID.fromString("00000000-0000-0000-0000-000000000002"));
        assertThatThrownBy(() -> confirmPayment.execute(created.id(), UUID.fromString("00000000-0000-0000-0000-000000000020"), UUID.fromString("00000000-0000-0000-0000-000000000002")))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void getPayment_byId() {
        var created = createPayment.execute(
                new CreatePaymentRequest(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), new BigDecimal("100"), "TND"), UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000001"));
        var found = getPayment.execute(created.id());
        assertThat(found.reference()).isEqualTo(created.reference());
    }

    @Test
    void getPayment_byReference() {
        var created = createPayment.execute(
                new CreatePaymentRequest(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), new BigDecimal("100"), "TND"), UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000001"));
        var found = getPayment.execute(created.reference());
        assertThat(found.id()).isEqualTo(created.id());
    }

    @Test
    void getPayment_notFound_throws() {
        assertThatThrownBy(() -> getPayment.execute(UUID.fromString("00000000-0000-0000-0000-000000000999")))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void listPayments_byShop() {
        createPayment.execute(new CreatePaymentRequest(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), new BigDecimal("100"), "TND"), UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000001"));
        createPayment.execute(new CreatePaymentRequest(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000003"), new BigDecimal("200"), "TND"), UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000001"));
        var result = listPayments.execute(UUID.fromString("00000000-0000-0000-0000-000000000001"), 0, 10);
        assertThat(result.items()).hasSize(2);
        assertThat(result.totalElements()).isEqualTo(2);
    }

    @Test
    void listPayments_bySupplier() {
        createPayment.execute(new CreatePaymentRequest(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), new BigDecimal("100"), "TND"), UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000001"));
        createPayment.execute(new CreatePaymentRequest(UUID.fromString("00000000-0000-0000-0000-000000000003"), UUID.fromString("00000000-0000-0000-0000-000000000002"), new BigDecimal("200"), "TND"), UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000002"));
        var result = listPayments.executeBySupplier(UUID.fromString("00000000-0000-0000-0000-000000000002"), 0, 10);
        assertThat(result.items()).hasSize(2);
    }

    @Test
    void listPayments_all() {
        createPayment.execute(new CreatePaymentRequest(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), new BigDecimal("100"), "TND"), UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000001"));
        var result = listPayments.executeAll(0, 10);
        assertThat(result.items()).hasSizeGreaterThanOrEqualTo(1);
    }

    @Test
    void listPayments_pagination() {
        for (int i = 0; i < 5; i++) {
            createPayment.execute(new CreatePaymentRequest(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), new BigDecimal("10"), "TND"), UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000001"));
        }
        var page0 = listPayments.execute(UUID.fromString("00000000-0000-0000-0000-000000000001"), 0, 2);
        assertThat(page0.items()).hasSize(2);
        assertThat(page0.totalPages()).isEqualTo(3);

        var page2 = listPayments.execute(UUID.fromString("00000000-0000-0000-0000-000000000001"), 2, 2);
        assertThat(page2.items()).hasSize(1);
    }

    @Test
    void stats_returnsCounts() {
        var stats = listPayments.stats();
        assertThat(stats.total()).isGreaterThanOrEqualTo(0);
    }
}
