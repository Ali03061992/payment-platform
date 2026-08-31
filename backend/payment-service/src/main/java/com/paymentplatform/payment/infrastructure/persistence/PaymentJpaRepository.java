package com.paymentplatform.payment.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PaymentJpaRepository extends JpaRepository<PaymentJpaEntity, Long> {
    Optional<PaymentJpaEntity> findByReference(String reference);
    List<PaymentJpaEntity> findByShopIdOrderByCreatedAtDesc(long shopId);
    List<PaymentJpaEntity> findBySupplierIdOrderByCreatedAtDesc(long supplierId);
    List<PaymentJpaEntity> findByStatusOrderByCreatedAtDesc(String status);
    long countByStatus(String status);
    boolean existsByReference(String reference);

    List<PaymentJpaEntity> findBySupplierIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            long supplierId, Instant from, Instant to);

    @Query("SELECT p.createdBy FROM PaymentJpaEntity p WHERE p.supplierId = :supplierId " +
           "AND p.createdAt BETWEEN :from AND :to GROUP BY p.createdBy")
    List<Long> findDistinctCreatedByBetween(@Param("supplierId") long supplierId,
                                            @Param("from") Instant from,
                                            @Param("to") Instant to);

    List<PaymentJpaEntity> findBySupplierIdAndCreatedByAndCreatedAtBetweenOrderByCreatedAtDesc(
            long supplierId, long createdBy, Instant from, Instant to);
}
