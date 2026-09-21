package com.paymentplatform.payment.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentEventJpaRepository extends JpaRepository<PaymentEventJpaEntity, UUID> {
    List<PaymentEventJpaEntity> findByPaymentIdOrderByTimestampAsc(UUID paymentId);
}
