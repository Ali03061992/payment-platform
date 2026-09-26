package com.paymentplatform.payment.application.usecase;

import com.paymentplatform.payment.application.dto.AgentPaymentSummary;
import com.paymentplatform.payment.application.dto.PaymentNameResolver;
import com.paymentplatform.payment.application.dto.PaymentResponse;
import com.paymentplatform.payment.infrastructure.persistence.PaymentJpaEntity;
import com.paymentplatform.payment.infrastructure.persistence.PaymentJpaRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AgentPaymentsBySupplierUseCase {

    private final PaymentJpaRepository paymentRepo;
    private final PaymentNameResolver nameResolver;

    public AgentPaymentsBySupplierUseCase(PaymentJpaRepository paymentRepo, PaymentNameResolver nameResolver) {
        this.paymentRepo = paymentRepo;
        this.nameResolver = nameResolver;
    }

    public List<AgentPaymentSummary> execute(UUID supplierId, Instant from, Instant to) {
        List<UUID> userIds = paymentRepo.findDistinctCreatedByBetween(supplierId, from, to);
        List<AgentPaymentSummary> summaries = new ArrayList<>();

        for (UUID userId : userIds) {
            List<PaymentJpaEntity> payments =
                    paymentRepo.findBySupplierIdAndCreatedByAndCreatedAtBetweenOrderByCreatedAtDesc(
                            supplierId, userId, from, to);

            BigDecimal total = payments.stream()
                    .map(PaymentJpaEntity::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal confirmedTotal = payments.stream()
                    .filter(p -> p.getStatus() == com.paymentplatform.payment.domain.model.PaymentStatus.CONFIRMED)
                    .map(PaymentJpaEntity::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            String currency = payments.isEmpty() ? "TND" : payments.get(0).getCurrency();

            List<PaymentResponse> responseList = payments.stream()
                    .map(e -> toResponse(e, userId))
                    .toList();

            String agentName = nameResolver.toNameResolver().resolveUser(userId);

            summaries.add(new AgentPaymentSummary(
                    userId,
                    agentName,
                    payments.size(),
                    total,
                    confirmedTotal,
                    currency,
                    responseList
            ));
        }

        return summaries;
    }

    private PaymentResponse toResponse(PaymentJpaEntity e, UUID agentUserId) {
        var batchResolver = nameResolver.toNameResolver();
        return new PaymentResponse(
                e.getId(), e.getReference(),
                e.getShopId(), batchResolver.resolveOrg(e.getShopId()),
                e.getSupplierId(), batchResolver.resolveOrg(e.getSupplierId()),
                e.getAmount(), e.getCurrency(), e.getStatus(), e.getRejectionReason(),
                e.getCreatedBy(), batchResolver.resolveUser(e.getCreatedBy()),
                null, null, null,
                agentUserId, LocalDate.now(),
                e.getVersion(), e.getCreatedAt(), e.getUpdatedAt(),
                List.of()
        );
    }
}
