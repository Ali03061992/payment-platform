package com.paymentplatform.payment.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentJpaRepository extends JpaRepository<PaymentJpaEntity, UUID> {
    Optional<PaymentJpaEntity> findByReference(String reference);
    List<PaymentJpaEntity> findByShopIdOrderByCreatedAtDesc(UUID shopId);
    List<PaymentJpaEntity> findBySupplierIdOrderByCreatedAtDesc(UUID supplierId);
    List<PaymentJpaEntity> findByStatusOrderByCreatedAtDesc(com.paymentplatform.payment.domain.model.PaymentStatus status);
    long countByStatus(com.paymentplatform.payment.domain.model.PaymentStatus status);
    boolean existsByReference(String reference);

    List<PaymentJpaEntity> findBySupplierIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            UUID supplierId, Instant from, Instant to);

    @Query("SELECT p FROM PaymentJpaEntity p WHERE p.status = 'PENDING' AND p.dueDate < :today")
    List<PaymentJpaEntity> findOverduePayments(@Param("today") java.time.LocalDate today);

    @Query("SELECT p FROM PaymentJpaEntity p WHERE p.status = 'PENDING' AND p.dueDate < :today AND p.supplierId = :supplierId")
    List<PaymentJpaEntity> findOverduePaymentsBySupplier(@Param("today") java.time.LocalDate today,
                                                          @Param("supplierId") UUID supplierId);

    @Query("SELECT p FROM PaymentJpaEntity p WHERE p.status = 'PENDING' AND p.dueDate < :today AND p.shopId = :shopId")
    List<PaymentJpaEntity> findOverduePaymentsByShop(@Param("today") java.time.LocalDate today,
                                                      @Param("shopId") UUID shopId);

    @Query("SELECT p.createdBy FROM PaymentJpaEntity p WHERE p.supplierId = :supplierId " +
           "AND p.createdAt BETWEEN :from AND :to GROUP BY p.createdBy")
    List<UUID> findDistinctCreatedByBetween(@Param("supplierId") UUID supplierId,
                                            @Param("from") Instant from,
                                            @Param("to") Instant to);

    List<PaymentJpaEntity> findBySupplierIdAndCreatedByAndCreatedAtBetweenOrderByCreatedAtDesc(
            UUID supplierId, UUID createdBy, Instant from, Instant to);
}
