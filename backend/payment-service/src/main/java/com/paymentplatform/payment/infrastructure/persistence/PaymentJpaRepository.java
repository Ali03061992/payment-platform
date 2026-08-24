package com.paymentplatform.payment.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentJpaRepository extends JpaRepository<PaymentJpaEntity, Long> {
    Optional<PaymentJpaEntity> findByReference(String reference);
    List<PaymentJpaEntity> findByShopIdOrderByCreatedAtDesc(long shopId);
    List<PaymentJpaEntity> findBySupplierIdOrderByCreatedAtDesc(long supplierId);
    List<PaymentJpaEntity> findByStatusOrderByCreatedAtDesc(String status);
    long countByStatus(String status);
    boolean existsByReference(String reference);
}
