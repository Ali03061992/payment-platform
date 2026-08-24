package com.paymentplatform.payment.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentEventJpaRepository extends JpaRepository<PaymentEventJpaEntity, Long> {
    List<PaymentEventJpaEntity> findByPaymentIdOrderByTimestampAsc(long paymentId);
}
