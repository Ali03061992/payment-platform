package com.paymentplatform.payment.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentEventJpaRepository extends JpaRepository<PaymentEventJpaEntity, UUID> {
    List<PaymentEventJpaEntity> findByPaymentIdOrderByTimestampAsc(UUID paymentId);
}
