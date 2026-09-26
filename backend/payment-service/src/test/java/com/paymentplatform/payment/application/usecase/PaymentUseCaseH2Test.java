package com.paymentplatform.payment.application.usecase;

import com.paymentplatform.payment.application.dto.CreatePaymentRequest;
import com.paymentplatform.payment.application.dto.RejectPaymentRequest;
import com.paymentplatform.payment.domain.model.PaymentStatus;
import com.paymentplatform.payment.domain.repository.PaymentRepository;
import com.paymentplatform.payment.infrastructure.http.TestOrganizationValidationConfig;
import com.paymentplatform.shared.domain.exception.DomainException;
import com.paymentplatform.shared.domain.exception.ForbiddenException;
import com.paymentplatform.shared.domain.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
/**
 * Tests de PaymentUseCaseH2Test.
 * Perimetre : cas d'usage/service PaymentUseCase sur base H2.
 * Moyens : contexte SpringBootTest, profil "test" (H2).
 */

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(TestOrganizationValidationConfig.class)
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
        var request = new CreatePaymentRequest(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), new BigDecimal("150.50"), "TND",null,null);
        var response = createPayment.execute(request, UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000001"));

        assertThat(response.id()).isNotNull();
        assertThat(response.reference()).startsWith("PAY-");
        assertThat(response.shopId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        assertThat(response.supplierId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("150.50"));
        assertThat(response.status()).isEqualTo(PaymentStatus.PENDING);
        assertThat(response.createdBy()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000010"));
    }

    @Test
    void confirmPayment_pendingToConfirmed() {
        var created = createPayment.execute(
                new CreatePaymentRequest(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), new BigDecimal("100"), "TND",null,null), UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000001"));
        var confirmed = confirmPayment.execute(created.id(), UUID.fromString("00000000-0000-0000-0000-000000000020"), UUID.fromString("00000000-0000-0000-0000-000000000002"));
        assertThat(confirmed.status()).isEqualTo(PaymentStatus.CONFIRMED);
    }

    @Test
    void confirmPayment_notFound_throws() {
        assertThatThrownBy(() -> confirmPayment.execute(UUID.fromString("00000000-0000-0000-0000-000000000999"), UUID.fromString("00000000-0000-0000-0000-000000000020"), UUID.fromString("00000000-0000-0000-0000-000000000002")))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void confirmPayment_wrongSupplier_throwsForbidden() {
        var created = createPayment.execute(
                new CreatePaymentRequest(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), new BigDecimal("100"), "TND",null,null), UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000001"));
        assertThatThrownBy(() -> confirmPayment.execute(created.id(), UUID.fromString("00000000-0000-0000-0000-000000000020"), UUID.fromString("00000000-0000-0000-0000-000000000099")))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void confirmPayment_nullOrgId_succeeds() {
        var created = createPayment.execute(
                new CreatePaymentRequest(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), new BigDecimal("100"), "TND",null,null), UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000001"));
        var confirmed = confirmPayment.execute(created.id(), UUID.fromString("00000000-0000-0000-0000-000000000020"), null);
        assertThat(confirmed.status()).isEqualTo(PaymentStatus.CONFIRMED);
    }

    @Test
    void rejectPayment_pendingToRejected() {
        var created = createPayment.execute(
                new CreatePaymentRequest(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), new BigDecimal("200"), "EUR",null,null), UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000001"));
        var rejected = rejectPayment.execute(created.id(),
                new RejectPaymentRequest("Montant incorrect"), UUID.fromString("00000000-0000-0000-0000-000000000020"), UUID.fromString("00000000-0000-0000-0000-000000000002"));
        assertThat(rejected.status()).isEqualTo(PaymentStatus.REJECTED);
        assertThat(rejected.rejectionReason()).isEqualTo("Montant incorrect");
    }

    @Test
    void rejectPayment_wrongSupplier_throwsForbidden() {
        var created = createPayment.execute(
                new CreatePaymentRequest(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), new BigDecimal("200"), "EUR",null,null), UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000001"));
        assertThatThrownBy(() -> rejectPayment.execute(created.id(),
                new RejectPaymentRequest("reason"), UUID.fromString("00000000-0000-0000-0000-000000000020"), UUID.fromString("00000000-0000-0000-0000-000000000099")))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void cancelPayment_pendingToCancelled() {
        var created = createPayment.execute(
                new CreatePaymentRequest(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), new BigDecimal("50"), "USD",null,null), UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000001"));
        var cancelled = cancelPayment.execute(created.id(), UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000001"));
        assertThat(cancelled.status()).isEqualTo(PaymentStatus.CANCELLED);
    }

    @Test
    void cancelPayment_bySupplier_succeeds() {
        var created = createPayment.execute(
                new CreatePaymentRequest(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), new BigDecimal("50"), "USD",null,null), UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000001"));
        var cancelled = cancelPayment.execute(created.id(), UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000002"));
        assertThat(cancelled.status()).isEqualTo(PaymentStatus.CANCELLED);
    }

    @Test
    void cancelPayment_wrongOrg_throwsForbidden() {
        var created = createPayment.execute(
                new CreatePaymentRequest(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), new BigDecimal("50"), "USD",null,null), UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000001"));
        assertThatThrownBy(() -> cancelPayment.execute(created.id(), UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000099")))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void cancelPayment_nullOrgId_succeeds() {
        var created = createPayment.execute(
                new CreatePaymentRequest(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), new BigDecimal("50"), "USD",null,null), UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000001"));
        var cancelled = cancelPayment.execute(created.id(), UUID.fromString("00000000-0000-0000-0000-000000000010"), null);
        assertThat(cancelled.status()).isEqualTo(PaymentStatus.CANCELLED);
    }

    @Test
    void confirmAlreadyConfirmed_throwsDomainException() {
        var created = createPayment.execute(
                new CreatePaymentRequest(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), new BigDecimal("100"), "TND",null,null), UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000001"));
        confirmPayment.execute(created.id(), UUID.fromString("00000000-0000-0000-0000-000000000020"), UUID.fromString("00000000-0000-0000-0000-000000000002"));
        assertThatThrownBy(() -> confirmPayment.execute(created.id(), UUID.fromString("00000000-0000-0000-0000-000000000020"), UUID.fromString("00000000-0000-0000-0000-000000000002")))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void getPayment_byId() {
        var created = createPayment.execute(
                new CreatePaymentRequest(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), new BigDecimal("100"), "TND",null,null), UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000001"));
        var found = getPayment.execute(created.id());
        assertThat(found.reference()).isEqualTo(created.reference());
    }

    @Test
    void getPayment_byReference() {
        var created = createPayment.execute(
                new CreatePaymentRequest(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), new BigDecimal("100"), "TND",null,null), UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000001"));
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
        createPayment.execute(new CreatePaymentRequest(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), new BigDecimal("100"), "TND",null,null), UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000001"));
        createPayment.execute(new CreatePaymentRequest(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000003"), new BigDecimal("200"), "TND",null,null), UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000001"));
        var result = listPayments.execute(UUID.fromString("00000000-0000-0000-0000-000000000001"), 0, 10);
        assertThat(result.items()).hasSize(2);
        assertThat(result.totalElements()).isEqualTo(2);
    }

    @Test
    void listPayments_bySupplier() {
        createPayment.execute(new CreatePaymentRequest(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), new BigDecimal("100"), "TND",null,null), UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000001"));
        createPayment.execute(new CreatePaymentRequest(UUID.fromString("00000000-0000-0000-0000-000000000003"), UUID.fromString("00000000-0000-0000-0000-000000000002"), new BigDecimal("200"), "TND",null,null), UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000002"));
        var result = listPayments.executeBySupplier(UUID.fromString("00000000-0000-0000-0000-000000000002"), 0, 10);
        assertThat(result.items()).hasSize(2);
    }

    @Test
    void listPayments_all() {
        createPayment.execute(new CreatePaymentRequest(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), new BigDecimal("100"), "TND",null,null), UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000001"));
        var result = listPayments.executeAll(0, 10);
        assertThat(result.items()).hasSizeGreaterThanOrEqualTo(1);
    }

    @Test
    void listPayments_pagination() {
        for (int i = 0; i < 5; i++) {
            createPayment.execute(new CreatePaymentRequest(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), new BigDecimal("10"), "TND",null,null), UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000001"));
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

    @Test
    void summarizeBySupplier_sqlAggregates_noFullLoad() {
        var shop = UUID.fromString("00000000-0000-0000-0000-000000000001");
        var supplier = UUID.fromString("00000000-0000-0000-0000-000000000002");
        var actor = UUID.fromString("00000000-0000-0000-0000-000000000010");
        var p1 = createPayment.execute(
                new CreatePaymentRequest(shop, supplier, new BigDecimal("100"), "TND", null, null), actor, shop);
        var p2 = createPayment.execute(
                new CreatePaymentRequest(shop, supplier, new BigDecimal("50"), "TND", null, null), actor, shop);
        confirmPayment.execute(p1.id(), UUID.fromString("00000000-0000-0000-0000-000000000020"), supplier);

        var summary = listPayments.summarizeBySupplier(supplier);

        assertThat(summary.confirmedCount()).isEqualTo(1);
        assertThat(summary.confirmedTotal()).isEqualByComparingTo(new BigDecimal("100"));
        assertThat(summary.pendingCount()).isEqualTo(1);
        assertThat(summary.pendingTotal()).isEqualByComparingTo(new BigDecimal("50"));
        assertThat(summary.rejectedCount()).isZero();
        assertThat(summary.rejectedTotal()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void createPayment_sameIdempotencyKey_returnsSamePaymentOnce() {
        var request = new CreatePaymentRequest(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), new BigDecimal("150.50"), "TND", null, null);
        UUID actor = UUID.fromString("00000000-0000-0000-0000-000000000010");
        UUID org = UUID.fromString("00000000-0000-0000-0000-000000000001");
        String key = "idem-key-" + UUID.randomUUID();

        var first = createPayment.execute(request, actor, org, key);
        var second = createPayment.execute(request, actor, org, key);

        assertThat(second.id()).isEqualTo(first.id());
        assertThat(payments.findByIdempotencyKey(key)).isPresent();
        assertThat(payments.findByIdempotencyKey(key).get().id()).isEqualTo(first.id());
        assertThat(payments.findAll()).hasSize(1);
    }

    @Test
    void createPayment_differentIdempotencyKeys_createsTwoPayments() {
        var request = new CreatePaymentRequest(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), new BigDecimal("150.50"), "TND", null, null);
        UUID actor = UUID.fromString("00000000-0000-0000-0000-000000000010");
        UUID org = UUID.fromString("00000000-0000-0000-0000-000000000001");

        var first = createPayment.execute(request, actor, org, "key-a-" + UUID.randomUUID());
        var second = createPayment.execute(request, actor, org, "key-b-" + UUID.randomUUID());

        assertThat(second.id()).isNotEqualTo(first.id());
        assertThat(payments.findAll()).hasSize(2);
    }
}
