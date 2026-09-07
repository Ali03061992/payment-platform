package com.paymentplatform.payment.application.usecase;

import com.paymentplatform.payment.application.dto.PageResponse;
import com.paymentplatform.payment.application.dto.PaymentNameResolver;
import com.paymentplatform.payment.application.dto.PaymentResponse;
import com.paymentplatform.payment.application.dto.PaymentStatsResponse;
import com.paymentplatform.payment.domain.model.Payment;
import com.paymentplatform.payment.domain.model.PaymentStatus;
import com.paymentplatform.payment.domain.repository.PaymentRepository;
import com.paymentplatform.payment.infrastructure.http.OrganizationValidationClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
    public PageResponse<PaymentResponse> execute(long shopId, int page, int size) {
        List<Payment> allPayments = payments.findByShopId(shopId);
        return paginate(allPayments, page, size);
    }

    @Transactional(readOnly = true)
    public PageResponse<PaymentResponse> executeBySupplier(long supplierId, int page, int size) {
        List<Payment> allPayments = payments.findBySupplierId(supplierId);
        return paginate(allPayments, page, size);
    }

    @Transactional(readOnly = true)
    public PageResponse<PaymentResponse> executeAll(int page, int size) {
        List<Payment> allPayments = payments.findAll();
        return paginate(allPayments, page, size);
    }

    private PageResponse<PaymentResponse> paginate(List<Payment> allPayments, int page, int size) {
        int start = Math.min(page * size, allPayments.size());
        int end = Math.min(start + size, allPayments.size());
        List<Payment> pageItems = allPayments.subList(start, end);

        List<PaymentResponse> responses = buildResponses(pageItems);
        int totalPages = (int) Math.ceil((double) allPayments.size() / size);
        return new PageResponse<>(responses, allPayments.size(), totalPages, page);
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
