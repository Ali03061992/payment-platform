package com.paymentplatform.organization.domain.valueobject;

import java.util.UUID;

import com.paymentplatform.shared.domain.exception.DomainException;

public record OrganizationId(UUID value) {
    public static OrganizationId of(UUID value) {
        return new OrganizationId(value);
    }
}
