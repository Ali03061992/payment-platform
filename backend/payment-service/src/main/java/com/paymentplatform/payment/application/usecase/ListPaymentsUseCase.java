package com.paymentplatform.payment.application.usecase;

import com.paymentplatform.payment.application.dto.PaymentNameResolver;
import com.paymentplatform.payment.application.dto.PaymentResponse;
import com.paymentplatform.payment.application.dto.PaymentStatsResponse;
import com.paymentplatform.payment.domain.model.Payment;
import com.paymentplatform.payment.domain.model.PaymentStatus;
import com.paymentplatform.payment.domain.repository.PaymentRepository;
import com.paymentplatform.payment.infrastructure.http.OrganizationValidationClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ListPaymentsUseCase {

    private final PaymentRepository payments;
    private final PaymentNameResolver nameResolver;
    private final OrganizationValidationClient orgClient;

    public ListPaymentsUseCase(PaymentRepository payments, PaymentNameResolver nameResolver,
                                OrganizationValidationClient orgClient) {
        this.payments = payments;
        this.nameResolver = nameResolver;
        this.orgClient = orgClient;
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> execute(long shopId) {
        List<Payment> domainPayments = payments.findByShopId(shopId);
        return buildResponses(domainPayments);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> executeBySupplier(long supplierId) {
        List<Payment> domainPayments = payments.findBySupplierId(supplierId);
        return buildResponses(domainPayments);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> executeAll() {
        List<Payment> domainPayments = payments.findAll();
        return buildResponses(domainPayments);
    }

    private List<PaymentResponse> buildResponses(List<Payment> domainPayments) {
        Set<Long> orgIds = new LinkedHashSet<>();
        Set<Long> userIds = new LinkedHashSet<>();

        for (Payment p : domainPayments) {
            orgIds.add(p.shopId());
            orgIds.add(p.supplierId());
            userIds.add(p.createdBy());
            for (var event : p.events()) {
                if (event.userId() != null) userIds.add(event.userId());
            }
        }

        Map<Long, String> orgNames = new HashMap<>();
        Map<Long, String> userNames = new HashMap<>();
        for (Long id : orgIds) {
            orgNames.put(id, orgClient.getOrganizationName(id).orElse("Org " + id));
        }
        for (Long id : userIds) {
            userNames.put(id, orgClient.getUserName(id).orElse("User " + id));
        }

        PaymentResponse.NameResolver resolver = new PaymentResponse.NameResolver() {
            @Override
            public String resolveOrg(long organizationId) {
                return orgNames.getOrDefault(organizationId, "Org " + organizationId);
            }
            @Override
            public String resolveUser(long userId) {
                return userNames.getOrDefault(userId, "User " + userId);
            }
        };

        return domainPayments.stream()
                .map(p -> PaymentResponse.from(p, resolver))
                .toList();
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
