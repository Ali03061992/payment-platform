package com.paymentplatform.payment.application.usecase;

import com.paymentplatform.payment.application.dto.AgentPaymentSummary;
import com.paymentplatform.payment.application.dto.PaymentResponse;
import com.paymentplatform.payment.infrastructure.persistence.PaymentJpaRepository;
import com.paymentplatform.payment.infrastructure.persistence.PaymentJpaEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
public class AgentPaymentsBySupplierUseCase {

    private final PaymentJpaRepository paymentRepo;

    public AgentPaymentsBySupplierUseCase(PaymentJpaRepository paymentRepo) {
        this.paymentRepo = paymentRepo;
    }

    public List<AgentPaymentSummary> execute(long supplierId, Instant from, Instant to) {
        List<Long> userIds = paymentRepo.findDistinctCreatedByBetween(supplierId, from, to);
        List<AgentPaymentSummary> summaries = new ArrayList<>();

        for (Long userId : userIds) {
            List<PaymentJpaEntity> payments =
                    paymentRepo.findBySupplierIdAndCreatedByAndCreatedAtBetweenOrderByCreatedAtDesc(
                            supplierId, userId, from, to);

            BigDecimal total = payments.stream()
                    .map(PaymentJpaEntity::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Instant todayStart = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC);
            Instant todayEnd = LocalDate.now().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

            BigDecimal confirmedToday = payments.stream()
                    .filter(p -> "CONFIRMED".equals(p.getStatus()))
                    .filter(p -> p.getCreatedAt().isAfter(todayStart) && p.getCreatedAt().isBefore(todayEnd))
                    .map(PaymentJpaEntity::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            String currency = payments.isEmpty() ? "TND" : payments.get(0).getCurrency();

            List<PaymentResponse> responseList = payments.stream()
                    .map(this::toResponse)
                    .toList();

            summaries.add(new AgentPaymentSummary(
                    userId,
                    "Agent #" + userId,
                    payments.size(),
                    total,
                    confirmedToday,
                    currency,
                    responseList
            ));
        }

        return summaries;
    }

    private PaymentResponse toResponse(PaymentJpaEntity e) {
        return new PaymentResponse(
                e.getId(), e.getReference(), e.getShopId(), e.getSupplierId(),
                e.getAmount(), e.getCurrency(), e.getStatus(), e.getRejectionReason(),
                e.getCreatedBy(), e.getVersion(), e.getCreatedAt(), e.getUpdatedAt(),
                List.of()
        );
    }
}
