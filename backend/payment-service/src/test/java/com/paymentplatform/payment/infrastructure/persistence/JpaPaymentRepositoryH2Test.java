package com.paymentplatform.payment.infrastructure.persistence;

import com.paymentplatform.payment.domain.model.Payment;
import com.paymentplatform.payment.domain.model.PaymentStatus;
import com.paymentplatform.payment.domain.valueobject.Money;
import com.paymentplatform.payment.domain.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class JpaPaymentRepositoryH2Test {

    @Autowired private PaymentRepository paymentRepository;

    @BeforeEach
    void setUp() {
        paymentRepository.findAll().forEach(p -> paymentRepository.save(p));
    }

    private Payment createAndSave(long shopId, long supplierId, long createdBy) {
        Payment p = Payment.create(shopId, supplierId,
                Money.of(new BigDecimal("100"), "TND"), createdBy);
        return paymentRepository.save(p);
    }

    @Test
    void save_andFindById() {
        Payment saved = createAndSave(1L, 2L, 10L);
        var found = paymentRepository.findById(saved.id());
        assertThat(found).isPresent();
        assertThat(found.get().reference()).isEqualTo(saved.reference());
    }

    @Test
    void findByReference() {
        Payment saved = createAndSave(1L, 2L, 10L);
        var found = paymentRepository.findByReference(saved.reference().value());
        assertThat(found).isPresent();
        assertThat(found.get().id()).isEqualTo(saved.id());
    }

    @Test
    void findByReference_notFound() {
        assertThat(paymentRepository.findByReference("NONEXISTENT")).isEmpty();
    }

    @Test
    void findAll() {
        createAndSave(1L, 2L, 10L);
        createAndSave(3L, 4L, 10L);
        var all = paymentRepository.findAll();
        assertThat(all).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void findByShopId() {
        createAndSave(1L, 2L, 10L);
        createAndSave(1L, 3L, 10L);
        createAndSave(5L, 2L, 10L);
        var result = paymentRepository.findByShopId(1L);
        assertThat(result).hasSize(2);
    }

    @Test
    void findBySupplierId() {
        createAndSave(1L, 2L, 10L);
        createAndSave(3L, 2L, 10L);
        createAndSave(5L, 4L, 10L);
        var result = paymentRepository.findBySupplierId(2L);
        assertThat(result).hasSize(2);
    }

    @Test
    void findByStatus() {
        Payment p = createAndSave(1L, 2L, 10L);
        paymentRepository.save(p.confirm(20L));
        var pending = paymentRepository.findByStatus(PaymentStatus.PENDING);
        var confirmed = paymentRepository.findByStatus(PaymentStatus.CONFIRMED);
        assertThat(confirmed).hasSizeGreaterThanOrEqualTo(1);
    }

    @Test
    void countByStatus() {
        createAndSave(1L, 2L, 10L);
        createAndSave(1L, 3L, 10L);
        long count = paymentRepository.countByStatus(PaymentStatus.PENDING);
        assertThat(count).isGreaterThanOrEqualTo(2);
    }

    @Test
    void existsByReference() {
        Payment saved = createAndSave(1L, 2L, 10L);
        assertThat(paymentRepository.existsByReference(saved.reference().value())).isTrue();
        assertThat(paymentRepository.existsByReference("FAKE")).isFalse();
    }

    @Test
    void save_updatesPayment() {
        Payment saved = createAndSave(1L, 2L, 10L);
        Payment confirmed = saved.confirm(20L);
        Payment updated = paymentRepository.save(confirmed);
        assertThat(updated.status()).isEqualTo(PaymentStatus.CONFIRMED);
    }
}
