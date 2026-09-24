package com.paymentplatform.payment;

import com.paymentplatform.payment.domain.model.Payment;
import com.paymentplatform.payment.domain.model.PaymentStatus;
import com.paymentplatform.payment.domain.valueobject.Money;
import com.paymentplatform.payment.domain.valueobject.PaymentReference;
import com.paymentplatform.payment.domain.valueobject.RejectionReason;
import com.paymentplatform.shared.domain.exception.DomainException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentTest {

    @Test
    void createPendingPayment() {
        Payment p = Payment.create(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), Money.of(new BigDecimal("150.50"), "TND"), UUID.fromString("00000000-0000-0000-0000-000000000010"));

        assertThat(p.id()).isNull();
        assertThat(p.reference().value()).startsWith("PAY-");
        assertThat(p.shopId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        assertThat(p.supplierId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        assertThat(p.money().amount()).isEqualByComparingTo(new BigDecimal("150.50"));
        assertThat(p.money().currency()).isEqualTo("TND");
        assertThat(p.status()).isEqualTo(PaymentStatus.PENDING);
        assertThat(p.rejectionReason()).isNull();
        assertThat(p.createdBy()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000010"));
        assertThat(p.version()).isEqualTo(0);
        assertThat(p.events()).hasSize(1);
    }

    @Test
    void confirmPayment() {
        Payment p = Payment.create(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), Money.of(new BigDecimal("100"), "TND"), UUID.fromString("00000000-0000-0000-0000-000000000010"));
        p = p.confirm(UUID.fromString("00000000-0000-0000-0000-000000000005"));

        assertThat(p.status()).isEqualTo(PaymentStatus.CONFIRMED);
        assertThat(p.events()).hasSize(2);
    }

    @Test
    void rejectPaymentWithReason() {
        Payment p = Payment.create(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), Money.of(new BigDecimal("200"), "EUR"), UUID.fromString("00000000-0000-0000-0000-000000000010"));
        p = p.reject(UUID.fromString("00000000-0000-0000-0000-000000000005"), new RejectionReason("Montant incorrect"));

        assertThat(p.status()).isEqualTo(PaymentStatus.REJECTED);
        assertThat(p.rejectionReason().value()).isEqualTo("Montant incorrect");
        assertThat(p.events()).hasSize(2);
    }

    @Test
    void cancelPayment() {
        Payment p = Payment.create(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), Money.of(new BigDecimal("50"), "USD"), UUID.fromString("00000000-0000-0000-0000-000000000010"));
        p = p.cancel(UUID.fromString("00000000-0000-0000-0000-000000000010"));

        assertThat(p.status()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(p.events()).hasSize(2);
    }

    @Test
    void cannotConfirmAlreadyConfirmed() {
        Payment confirmed = Payment.create(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), Money.of(new BigDecimal("100"), "TND"), UUID.fromString("00000000-0000-0000-0000-000000000010")).confirm(UUID.fromString("00000000-0000-0000-0000-000000000005"));

        assertThatThrownBy(() -> confirmed.confirm(UUID.fromString("00000000-0000-0000-0000-000000000005")))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("état terminal");
    }

    @Test
    void cannotRejectAlreadyCancelled() {
        Payment cancelled = Payment.create(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), Money.of(new BigDecimal("100"), "TND"), UUID.fromString("00000000-0000-0000-0000-000000000010")).cancel(UUID.fromString("00000000-0000-0000-0000-000000000010"));

        assertThatThrownBy(() -> cancelled.reject(UUID.fromString("00000000-0000-0000-0000-000000000005"), new RejectionReason("test")))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("état terminal");
    }

    @Test
    void cannotConfirmAlreadyRejected() {
        Payment rejected = Payment.create(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), Money.of(new BigDecimal("100"), "TND"), UUID.fromString("00000000-0000-0000-0000-000000000010")).reject(UUID.fromString("00000000-0000-0000-0000-000000000005"), new RejectionReason("reason"));

        assertThatThrownBy(() -> rejected.confirm(UUID.fromString("00000000-0000-0000-0000-000000000005")))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("état terminal");
    }

    @Test
    void cannotCreateWithZeroAmount() {
        assertThatThrownBy(() -> Payment.create(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), Money.of(BigDecimal.ZERO, "TND"), UUID.fromString("00000000-0000-0000-0000-000000000010")))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("supérieur ou égal à 0.01");
    }

    @Test
    void cannotCreateWithNegativeAmount() {
        assertThatThrownBy(() -> Payment.create(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), Money.of(new BigDecimal("-10"), "TND"), UUID.fromString("00000000-0000-0000-0000-000000000010")))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("supérieur ou égal à 0.01");
    }

    @Test
    void cannotCreateWithAmountExceedingMaximum() {
        assertThatThrownBy(() -> Payment.create(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), Money.of(new BigDecimal("1000000.00"), "TND"), UUID.fromString("00000000-0000-0000-0000-000000000010")))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("ne doit pas dépasser 999999.99");
    }

    @Test
    void cannotCreateWithSameShopAndSupplier() {
        assertThatThrownBy(() -> Payment.create(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000001"), Money.of(new BigDecimal("100"), "TND"), UUID.fromString("00000000-0000-0000-0000-000000000010")))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("différents");
    }

    @Test
    void paymentReferenceFormat() {
        PaymentReference ref = PaymentReference.generate();
        assertThat(ref.value()).matches("PAY-\\d+-\\d{6}");
    }

    @Test
    void moneyNormalizesCurrency() {
        Money m = Money.of(new BigDecimal("100"), "tnd");
        assertThat(m.currency()).isEqualTo("TND");
    }

    @Test
    void rejectionReasonValidation() {
        assertThatThrownBy(() -> new RejectionReason(""))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> new RejectionReason(" ".repeat(10)))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void belongsToShop() {
        Payment p = Payment.create(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), Money.of(new BigDecimal("100"), "TND"), UUID.fromString("00000000-0000-0000-0000-000000000010"));
        assertThat(p.belongsToShop(UUID.fromString("00000000-0000-0000-0000-000000000001"))).isTrue();
        assertThat(p.belongsToShop(UUID.fromString("00000000-0000-0000-0000-000000000002"))).isFalse();
    }

    @Test
    void belongsToSupplier() {
        Payment p = Payment.create(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), Money.of(new BigDecimal("100"), "TND"), UUID.fromString("00000000-0000-0000-0000-000000000010"));
        assertThat(p.belongsToSupplier(UUID.fromString("00000000-0000-0000-0000-000000000002"))).isTrue();
        assertThat(p.belongsToSupplier(UUID.fromString("00000000-0000-0000-0000-000000000001"))).isFalse();
    }

    @Test
    void canBeViewedBy() {
        Payment p = Payment.create(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), Money.of(new BigDecimal("100"), "TND"), UUID.fromString("00000000-0000-0000-0000-000000000010"));
        assertThat(p.canBeViewedBy(UUID.fromString("00000000-0000-0000-0000-000000000010"), null)).isTrue();
        assertThat(p.canBeViewedBy(UUID.fromString("00000000-0000-0000-0000-000000000099"), UUID.fromString("00000000-0000-0000-0000-000000000001"))).isTrue();
        assertThat(p.canBeViewedBy(UUID.fromString("00000000-0000-0000-0000-000000000099"), UUID.fromString("00000000-0000-0000-0000-000000000002"))).isTrue();
        assertThat(p.canBeViewedBy(UUID.fromString("00000000-0000-0000-0000-000000000099"), UUID.fromString("00000000-0000-0000-0000-000000000003"))).isFalse();
    }
}
