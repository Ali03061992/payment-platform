package com.paymentplatform.organization.domain.valueobject;

import com.paymentplatform.shared.domain.exception.DomainException;

public record OrganizationId(long value) {
    public static OrganizationId of(long value) {
        return new OrganizationId(value);
    }
}
