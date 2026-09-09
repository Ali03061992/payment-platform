package com.paymentplatform.payment.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PaymentJpaRepository extends JpaRepository<PaymentJpaEntity, UUID> {
    Optional<PaymentJpaEntity> findByReference(String reference);
    List<PaymentJpaEntity> findByShopIdOrderByCreatedAtDesc(UUID shopId);
    List<PaymentJpaEntity> findBySupplierIdOrderByCreatedAtDesc(UUID supplierId);
    List<PaymentJpaEntity> findByStatusOrderByCreatedAtDesc(String status);
    long countByStatus(String status);
    boolean existsByReference(String reference);

    List<PaymentJpaEntity> findBySupplierIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            UUID supplierId, Instant from, Instant to);

    @Query("SELECT p.createdBy FROM PaymentJpaEntity p WHERE p.supplierId = :supplierId " +
           "AND p.createdAt BETWEEN :from AND :to GROUP BY p.createdBy")
    List<UUID> findDistinctCreatedByBetween(@Param("supplierId") UUID supplierId,
                                            @Param("from") Instant from,
                                            @Param("to") Instant to);

    List<PaymentJpaEntity> findBySupplierIdAndCreatedByAndCreatedAtBetweenOrderByCreatedAtDesc(
            UUID supplierId, UUID createdBy, Instant from, Instant to);
}
