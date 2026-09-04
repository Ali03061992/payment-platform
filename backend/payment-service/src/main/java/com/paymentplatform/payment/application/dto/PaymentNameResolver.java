package com.paymentplatform.payment.application.dto;

import com.paymentplatform.payment.infrastructure.http.OrganizationValidationClient;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PaymentNameResolver {

    private final OrganizationValidationClient orgClient;

    public PaymentNameResolver(OrganizationValidationClient orgClient) {
        this.orgClient = orgClient;
    }

    public PaymentResponse.NameResolver toNameResolver() {
        return new PaymentResponse.NameResolver() {
            @Override
            public String resolveOrg(long organizationId) {
                return orgClient.getOrganizationName(organizationId).orElse("Org " + organizationId);
            }

            @Override
            public String resolveUser(long userId) {
                return orgClient.getUserName(userId).orElse("User " + userId);
            }
        };
    }

    public PaymentResponse.NameResolver toBatchNameResolver(List<? extends PaymentResponse> payments) {
        Set<Long> orgIds = new LinkedHashSet<>();
        Set<Long> userIds = new LinkedHashSet<>();

        for (PaymentResponse p : payments) {
            orgIds.add(p.shopId());
            orgIds.add(p.supplierId());
            userIds.add(p.createdBy());
            if (p.confirmedByName() == null) userIds.add(p.createdBy());
            if (p.rejectedByName() == null) userIds.add(p.createdBy());
            if (p.cancelledByName() == null) userIds.add(p.createdBy());
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

        return new PaymentResponse.NameResolver() {
            @Override
            public String resolveOrg(long organizationId) {
                return orgNames.getOrDefault(organizationId, "Org " + organizationId);
            }

            @Override
            public String resolveUser(long userId) {
                return userNames.getOrDefault(userId, "User " + userId);
            }
        };
    }
}
