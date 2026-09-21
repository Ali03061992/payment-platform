package com.paymentplatform.payment.infrastructure.persistence;

import com.paymentplatform.payment.domain.model.Payment;
import com.paymentplatform.payment.domain.model.PaymentStatus;
import com.paymentplatform.payment.domain.repository.PaymentRepository;
import com.paymentplatform.payment.domain.valueobject.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

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

    private Payment createAndSave(UUID shopId, UUID supplierId, UUID createdBy) {
        Payment p = Payment.create(shopId, supplierId,
                Money.of(new BigDecimal("100"), "TND"), createdBy);
        return paymentRepository.save(p);
    }

    @Test
    void save_andFindById() {
        Payment saved = createAndSave(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), UUID.fromString("00000000-0000-0000-0000-000000000010"));
        var found = paymentRepository.findById(saved.id());
        assertThat(found).isPresent();
        assertThat(found.get().reference()).isEqualTo(saved.reference());
    }

    @Test
    void findByReference() {
        Payment saved = createAndSave(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), UUID.fromString("00000000-0000-0000-0000-000000000010"));
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
        createAndSave(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), UUID.fromString("00000000-0000-0000-0000-000000000010"));
        createAndSave(UUID.fromString("00000000-0000-0000-0000-000000000003"), UUID.fromString("00000000-0000-0000-0000-000000000004"), UUID.fromString("00000000-0000-0000-0000-000000000010"));
        var all = paymentRepository.findAll();
        assertThat(all).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void findByShopId() {
        createAndSave(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), UUID.fromString("00000000-0000-0000-0000-000000000010"));
        createAndSave(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000003"), UUID.fromString("00000000-0000-0000-0000-000000000010"));
        createAndSave(UUID.fromString("00000000-0000-0000-0000-000000000005"), UUID.fromString("00000000-0000-0000-0000-000000000002"), UUID.fromString("00000000-0000-0000-0000-000000000010"));
        var result = paymentRepository.findByShopId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        assertThat(result).hasSize(2);
    }

    @Test
    void findBySupplierId() {
        createAndSave(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), UUID.fromString("00000000-0000-0000-0000-000000000010"));
        createAndSave(UUID.fromString("00000000-0000-0000-0000-000000000003"), UUID.fromString("00000000-0000-0000-0000-000000000002"), UUID.fromString("00000000-0000-0000-0000-000000000010"));
        createAndSave(UUID.fromString("00000000-0000-0000-0000-000000000005"), UUID.fromString("00000000-0000-0000-0000-000000000004"), UUID.fromString("00000000-0000-0000-0000-000000000010"));
        var result = paymentRepository.findBySupplierId(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        assertThat(result).hasSize(2);
    }

    @Test
    void findByStatus() {
        Payment p = createAndSave(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), UUID.fromString("00000000-0000-0000-0000-000000000010"));
        paymentRepository.save(p.confirm(UUID.fromString("00000000-0000-0000-0000-000000000020")));
        var pending = paymentRepository.findByStatus(PaymentStatus.PENDING);
        var confirmed = paymentRepository.findByStatus(PaymentStatus.CONFIRMED);
        assertThat(confirmed).hasSizeGreaterThanOrEqualTo(1);
    }

    @Test
    void countByStatus() {
        createAndSave(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), UUID.fromString("00000000-0000-0000-0000-000000000010"));
        createAndSave(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000003"), UUID.fromString("00000000-0000-0000-0000-000000000010"));
        long count = paymentRepository.countByStatus(PaymentStatus.PENDING);
        assertThat(count).isGreaterThanOrEqualTo(2);
    }

    @Test
    void existsByReference() {
        Payment saved = createAndSave(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), UUID.fromString("00000000-0000-0000-0000-000000000010"));
        assertThat(paymentRepository.existsByReference(saved.reference().value())).isTrue();
        assertThat(paymentRepository.existsByReference("FAKE")).isFalse();
    }

    @Test
    void save_updatesPayment() {
        Payment saved = createAndSave(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), UUID.fromString("00000000-0000-0000-0000-000000000010"));
        Payment confirmed = saved.confirm(UUID.fromString("00000000-0000-0000-0000-000000000020"));
        Payment updated = paymentRepository.save(confirmed);
        assertThat(updated.status()).isEqualTo(PaymentStatus.CONFIRMED);
    }
}
