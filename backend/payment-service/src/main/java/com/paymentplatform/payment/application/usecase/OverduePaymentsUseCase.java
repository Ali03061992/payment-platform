package com.paymentplatform.payment.application.usecase;

import com.paymentplatform.payment.application.dto.PageResponse;
import com.paymentplatform.payment.application.dto.PaymentNameResolver;
import com.paymentplatform.payment.application.dto.PaymentResponse;
import com.paymentplatform.payment.domain.model.Payment;
import com.paymentplatform.payment.domain.repository.PaymentRepository;
import com.paymentplatform.payment.infrastructure.http.OrganizationValidationClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
public class OverduePaymentsUseCase {

    private final PaymentRepository payments;
    private final PaymentNameResolver nameResolver;
    private final OrganizationValidationClient orgClient;

    public OverduePaymentsUseCase(PaymentRepository payments, PaymentNameResolver nameResolver,
                                   OrganizationValidationClient orgClient) {
        this.payments = payments;
        this.nameResolver = nameResolver;
        this.orgClient = orgClient;
    }

    @Transactional(readOnly = true)
    public PageResponse<PaymentResponse> execute(UUID userId, UUID organizationId,
                                                  String role, int page, int size) {
        LocalDate today = LocalDate.now();
        List<Payment> overdue;

        if (role != null && role.contains("SYSTEM_ADMIN")) {
            overdue = payments.findOverdue(today);
        } else if (organizationId != null && role != null) {
            if (role.contains("SHOP")) {
                overdue = payments.findOverdueByShop(today, organizationId);
            } else if (role.contains("SUPPLIER")) {
                overdue = payments.findOverdueBySupplier(today, organizationId);
            } else {
                overdue = payments.findOverdue(today);
            }
        } else {
            overdue = payments.findOverdue(today);
        }

        int start = Math.min(page * size, overdue.size());
        int end = Math.min(start + size, overdue.size());
        List<Payment> pageItems = overdue.subList(start, end);

        List<PaymentResponse> responses = buildResponses(pageItems);
        int totalPages = (int) Math.ceil((double) overdue.size() / size);
        return new PageResponse<>(responses, overdue.size(), totalPages, page);
    }

    private List<PaymentResponse> buildResponses(List<Payment> domainPayments) {
        Set<UUID> orgIds = new LinkedHashSet<>();
        Set<UUID> userIds = new LinkedHashSet<>();

        for (Payment p : domainPayments) {
            orgIds.add(p.shopId());
            orgIds.add(p.supplierId());
            userIds.add(p.createdBy());
            for (var event : p.events()) {
                if (event.userId() != null) userIds.add(event.userId());
            }
        }

        Map<UUID, String> orgNames = new HashMap<>();
        Map<UUID, String> userNames = new HashMap<>();
        for (UUID id : orgIds) {
            orgNames.put(id, orgClient.getOrganizationName(id).orElse("Org " + id));
        }
        for (UUID id : userIds) {
            userNames.put(id, orgClient.getUserName(id).orElse("User " + id));
        }

        PaymentResponse.NameResolver resolver = new PaymentResponse.NameResolver() {
            @Override
            public String resolveOrg(UUID organizationId) {
                return orgNames.getOrDefault(organizationId, "Org " + organizationId);
            }
            @Override
            public String resolveUser(UUID userId) {
                return userNames.getOrDefault(userId, "User " + userId);
            }
        };

        return domainPayments.stream()
                .map(p -> PaymentResponse.from(p, resolver))
                .toList();
    }
}
