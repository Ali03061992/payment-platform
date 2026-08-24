package com.paymentplatform.payment.application.usecase;

import com.paymentplatform.payment.application.dto.PaymentResponse;
import com.paymentplatform.payment.application.dto.PaymentStatsResponse;
import com.paymentplatform.payment.domain.model.Payment;
import com.paymentplatform.payment.domain.model.PaymentStatus;
import com.paymentplatform.payment.domain.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ListPaymentsUseCase {

    private final PaymentRepository payments;

    public ListPaymentsUseCase(PaymentRepository payments) {
        this.payments = payments;
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> execute(long shopId) {
        return payments.findByShopId(shopId).stream()
                .map(PaymentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> executeBySupplier(long supplierId) {
        return payments.findBySupplierId(supplierId).stream()
                .map(PaymentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> executeAll() {
        return payments.findAll().stream()
                .map(PaymentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PaymentStatsResponse stats() {
        return new PaymentStatsResponse(
                payments.countByStatus(PaymentStatus.PENDING)
                        + payments.countByStatus(PaymentStatus.CONFIRMED)
                        + payments.countByStatus(PaymentStatus.REJECTED)
                        + payments.countByStatus(PaymentStatus.CANCELLED),
                payments.countByStatus(PaymentStatus.PENDING),
                payments.countByStatus(PaymentStatus.CONFIRMED),
                payments.countByStatus(PaymentStatus.REJECTED),
                payments.countByStatus(PaymentStatus.CANCELLED)
        );
    }
}
