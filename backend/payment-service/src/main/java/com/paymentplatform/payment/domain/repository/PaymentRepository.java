package com.paymentplatform.payment.domain.repository;

import com.paymentplatform.payment.domain.model.Payment;
import com.paymentplatform.payment.domain.model.PaymentStatus;
import com.paymentplatform.payment.domain.model.PaymentStatusSummary;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository {
    Payment save(Payment payment);
    Optional<Payment> findById(UUID id);
    Optional<Payment> findByReference(String reference);
    List<Payment> findAll();
    List<Payment> findByShopId(UUID shopId);
    List<Payment> findBySupplierId(UUID supplierId);
    List<Payment> findByStatus(PaymentStatus status);
    long countByStatus(PaymentStatus status);
    boolean existsByReference(String reference);
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
    /**
     * B5 : agrégats (COUNT/SUM) par statut calculés en base, sans charger les paiements.
     */
    List<PaymentStatusSummary> summarizeBySupplier(UUID supplierId);
    List<Payment> findOverdue(LocalDate today);
    List<Payment> findOverdueBySupplier(LocalDate today, UUID supplierId);
    List<Payment> findOverdueByShop(LocalDate today, UUID shopId);
}
