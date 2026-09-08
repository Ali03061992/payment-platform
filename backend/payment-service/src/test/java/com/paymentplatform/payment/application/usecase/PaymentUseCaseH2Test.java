package com.paymentplatform.payment.application.usecase;

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
        var request = new CreatePaymentRequest(1L, 2L, new BigDecimal("150.50"), "TND");
        var response = createPayment.execute(request, 10L, 1L);

        assertThat(response.id()).isNotNull();
        assertThat(response.reference()).startsWith("PAY-");
        assertThat(response.shopId()).isEqualTo(1L);
        assertThat(response.supplierId()).isEqualTo(2L);
        assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("150.50"));
        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.createdBy()).isEqualTo(10L);
    }

    @Test
    void confirmPayment_pendingToConfirmed() {
        var created = createPayment.execute(
                new CreatePaymentRequest(1L, 2L, new BigDecimal("100"), "TND"), 10L, 1L);
        var confirmed = confirmPayment.execute(created.id(), 20L, 2L);
        assertThat(confirmed.status()).isEqualTo("CONFIRMED");
    }

    @Test
    void confirmPayment_notFound_throws() {
        assertThatThrownBy(() -> confirmPayment.execute(999L, 20L, 2L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void confirmPayment_wrongSupplier_throwsForbidden() {
        var created = createPayment.execute(
                new CreatePaymentRequest(1L, 2L, new BigDecimal("100"), "TND"), 10L, 1L);
        assertThatThrownBy(() -> confirmPayment.execute(created.id(), 20L, 99L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void confirmPayment_nullOrgId_succeeds() {
        var created = createPayment.execute(
                new CreatePaymentRequest(1L, 2L, new BigDecimal("100"), "TND"), 10L, 1L);
        var confirmed = confirmPayment.execute(created.id(), 20L, null);
        assertThat(confirmed.status()).isEqualTo("CONFIRMED");
    }

    @Test
    void rejectPayment_pendingToRejected() {
        var created = createPayment.execute(
                new CreatePaymentRequest(1L, 2L, new BigDecimal("200"), "EUR"), 10L, 1L);
        var rejected = rejectPayment.execute(created.id(),
                new RejectPaymentRequest("Montant incorrect"), 20L, 2L);
        assertThat(rejected.status()).isEqualTo("REJECTED");
        assertThat(rejected.rejectionReason()).isEqualTo("Montant incorrect");
    }

    @Test
    void rejectPayment_wrongSupplier_throwsForbidden() {
        var created = createPayment.execute(
                new CreatePaymentRequest(1L, 2L, new BigDecimal("200"), "EUR"), 10L, 1L);
        assertThatThrownBy(() -> rejectPayment.execute(created.id(),
                new RejectPaymentRequest("reason"), 20L, 99L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void cancelPayment_pendingToCancelled() {
        var created = createPayment.execute(
                new CreatePaymentRequest(1L, 2L, new BigDecimal("50"), "USD"), 10L, 1L);
        var cancelled = cancelPayment.execute(created.id(), 10L, 1L);
        assertThat(cancelled.status()).isEqualTo("CANCELLED");
    }

    @Test
    void cancelPayment_bySupplier_succeeds() {
        var created = createPayment.execute(
                new CreatePaymentRequest(1L, 2L, new BigDecimal("50"), "USD"), 10L, 1L);
        var cancelled = cancelPayment.execute(created.id(), 10L, 2L);
        assertThat(cancelled.status()).isEqualTo("CANCELLED");
    }

    @Test
    void cancelPayment_wrongOrg_throwsForbidden() {
        var created = createPayment.execute(
                new CreatePaymentRequest(1L, 2L, new BigDecimal("50"), "USD"), 10L, 1L);
        assertThatThrownBy(() -> cancelPayment.execute(created.id(), 10L, 99L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void cancelPayment_nullOrgId_succeeds() {
        var created = createPayment.execute(
                new CreatePaymentRequest(1L, 2L, new BigDecimal("50"), "USD"), 10L, 1L);
        var cancelled = cancelPayment.execute(created.id(), 10L, null);
        assertThat(cancelled.status()).isEqualTo("CANCELLED");
    }

    @Test
    void confirmAlreadyConfirmed_throwsDomainException() {
        var created = createPayment.execute(
                new CreatePaymentRequest(1L, 2L, new BigDecimal("100"), "TND"), 10L, 1L);
        confirmPayment.execute(created.id(), 20L, 2L);
        assertThatThrownBy(() -> confirmPayment.execute(created.id(), 20L, 2L))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void getPayment_byId() {
        var created = createPayment.execute(
                new CreatePaymentRequest(1L, 2L, new BigDecimal("100"), "TND"), 10L, 1L);
        var found = getPayment.execute(created.id());
        assertThat(found.reference()).isEqualTo(created.reference());
    }

    @Test
    void getPayment_byReference() {
        var created = createPayment.execute(
                new CreatePaymentRequest(1L, 2L, new BigDecimal("100"), "TND"), 10L, 1L);
        var found = getPayment.execute(created.reference());
        assertThat(found.id()).isEqualTo(created.id());
    }

    @Test
    void getPayment_notFound_throws() {
        assertThatThrownBy(() -> getPayment.execute(999L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void listPayments_byShop() {
        createPayment.execute(new CreatePaymentRequest(1L, 2L, new BigDecimal("100"), "TND"), 10L, 1L);
        createPayment.execute(new CreatePaymentRequest(1L, 3L, new BigDecimal("200"), "TND"), 10L, 1L);
        var result = listPayments.execute(1L, 0, 10);
        assertThat(result.items()).hasSize(2);
        assertThat(result.totalElements()).isEqualTo(2);
    }

    @Test
    void listPayments_bySupplier() {
        createPayment.execute(new CreatePaymentRequest(1L, 2L, new BigDecimal("100"), "TND"), 10L, 1L);
        createPayment.execute(new CreatePaymentRequest(3L, 2L, new BigDecimal("200"), "TND"), 10L, 2L);
        var result = listPayments.executeBySupplier(2L, 0, 10);
        assertThat(result.items()).hasSize(2);
    }

    @Test
    void listPayments_all() {
        createPayment.execute(new CreatePaymentRequest(1L, 2L, new BigDecimal("100"), "TND"), 10L, 1L);
        var result = listPayments.executeAll(0, 10);
        assertThat(result.items()).hasSizeGreaterThanOrEqualTo(1);
    }

    @Test
    void listPayments_pagination() {
        for (int i = 0; i < 5; i++) {
            createPayment.execute(new CreatePaymentRequest(1L, 2L, new BigDecimal("10"), "TND"), 10L, 1L);
        }
        var page0 = listPayments.execute(1L, 0, 2);
        assertThat(page0.items()).hasSize(2);
        assertThat(page0.totalPages()).isEqualTo(3);

        var page2 = listPayments.execute(1L, 2, 2);
        assertThat(page2.items()).hasSize(1);
    }

    @Test
    void stats_returnsCounts() {
        var stats = listPayments.stats();
        assertThat(stats.total()).isGreaterThanOrEqualTo(0);
    }
}
