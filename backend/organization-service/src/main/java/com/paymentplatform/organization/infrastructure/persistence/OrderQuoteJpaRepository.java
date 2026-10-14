package com.paymentplatform.organization.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderQuoteJpaRepository extends JpaRepository<OrderQuoteJpaEntity, UUID> {
    List<OrderQuoteJpaEntity> findByOrderIdOrderByVersionAsc(UUID orderId);
}
