package com.paymentplatform.payment.domain.repository;

import java.util.UUID;

import com.paymentplatform.payment.domain.model.Payment;
import com.paymentplatform.payment.domain.model.PaymentStatus;

import java.util.List;
import java.util.Optional;

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
}
