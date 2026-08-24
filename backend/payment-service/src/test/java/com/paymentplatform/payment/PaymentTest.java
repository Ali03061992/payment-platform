package com.paymentplatform.payment;

import com.paymentplatform.shared.domain.exception.DomainException;
import com.paymentplatform.payment.domain.model.Payment;
import com.paymentplatform.payment.domain.model.PaymentStatus;
import com.paymentplatform.payment.domain.valueobject.Money;
import com.paymentplatform.payment.domain.valueobject.PaymentReference;
import com.paymentplatform.payment.domain.valueobject.RejectionReason;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class PaymentTest {

    @Test
    void createPendingPayment() {
        Payment p = Payment.create(1L, 2L, Money.of(new BigDecimal("150.50"), "TND"), 10L);

        assertThat(p.id()).isNull();
        assertThat(p.reference().value()).startsWith("PAY-");
        assertThat(p.shopId()).isEqualTo(1L);
        assertThat(p.supplierId()).isEqualTo(2L);
        assertThat(p.money().amount()).isEqualByComparingTo(new BigDecimal("150.50"));
        assertThat(p.money().currency()).isEqualTo("TND");
        assertThat(p.status()).isEqualTo(PaymentStatus.PENDING);
        assertThat(p.rejectionReason()).isNull();
        assertThat(p.createdBy()).isEqualTo(10L);
        assertThat(p.version()).isEqualTo(0);
        assertThat(p.events()).hasSize(1);
    }

    @Test
    void confirmPayment() {
        Payment p = Payment.create(1L, 2L, Money.of(new BigDecimal("100"), "TND"), 10L);
        p = p.confirm(5L);

        assertThat(p.status()).isEqualTo(PaymentStatus.CONFIRMED);
        assertThat(p.events()).hasSize(2);
    }

    @Test
    void rejectPaymentWithReason() {
        Payment p = Payment.create(1L, 2L, Money.of(new BigDecimal("200"), "EUR"), 10L);
        p = p.reject(5L, new RejectionReason("Montant incorrect"));

        assertThat(p.status()).isEqualTo(PaymentStatus.REJECTED);
        assertThat(p.rejectionReason().value()).isEqualTo("Montant incorrect");
        assertThat(p.events()).hasSize(2);
    }

    @Test
    void cancelPayment() {
        Payment p = Payment.create(1L, 2L, Money.of(new BigDecimal("50"), "USD"), 10L);
        p = p.cancel(10L);

        assertThat(p.status()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(p.events()).hasSize(2);
    }

    @Test
    void cannotConfirmAlreadyConfirmed() {
        Payment confirmed = Payment.create(1L, 2L, Money.of(new BigDecimal("100"), "TND"), 10L).confirm(5L);

        assertThatThrownBy(() -> confirmed.confirm(5L))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("état terminal");
    }

    @Test
    void cannotRejectAlreadyCancelled() {
        Payment cancelled = Payment.create(1L, 2L, Money.of(new BigDecimal("100"), "TND"), 10L).cancel(10L);

        assertThatThrownBy(() -> cancelled.reject(5L, new RejectionReason("test")))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("état terminal");
    }

    @Test
    void cannotConfirmAlreadyRejected() {
        Payment rejected = Payment.create(1L, 2L, Money.of(new BigDecimal("100"), "TND"), 10L).reject(5L, new RejectionReason("reason"));

        assertThatThrownBy(() -> rejected.confirm(5L))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("état terminal");
    }

    @Test
    void cannotCreateWithZeroAmount() {
        assertThatThrownBy(() -> Payment.create(1L, 2L, Money.of(BigDecimal.ZERO, "TND"), 10L))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("supérieur à 0");
    }

    @Test
    void cannotCreateWithNegativeAmount() {
        assertThatThrownBy(() -> Payment.create(1L, 2L, Money.of(new BigDecimal("-10"), "TND"), 10L))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("supérieur à 0");
    }

    @Test
    void cannotCreateWithSameShopAndSupplier() {
        assertThatThrownBy(() -> Payment.create(1L, 1L, Money.of(new BigDecimal("100"), "TND"), 10L))
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
        Payment p = Payment.create(1L, 2L, Money.of(new BigDecimal("100"), "TND"), 10L);
        assertThat(p.belongsToShop(1L)).isTrue();
        assertThat(p.belongsToShop(2L)).isFalse();
    }

    @Test
    void belongsToSupplier() {
        Payment p = Payment.create(1L, 2L, Money.of(new BigDecimal("100"), "TND"), 10L);
        assertThat(p.belongsToSupplier(2L)).isTrue();
        assertThat(p.belongsToSupplier(1L)).isFalse();
    }

    @Test
    void canBeViewedBy() {
        Payment p = Payment.create(1L, 2L, Money.of(new BigDecimal("100"), "TND"), 10L);
        assertThat(p.canBeViewedBy(10L, null)).isTrue();
        assertThat(p.canBeViewedBy(99L, 1L)).isTrue();
        assertThat(p.canBeViewedBy(99L, 2L)).isTrue();
        assertThat(p.canBeViewedBy(99L, 3L)).isFalse();
    }
}
