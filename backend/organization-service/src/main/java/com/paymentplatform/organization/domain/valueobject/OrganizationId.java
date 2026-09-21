package com.paymentplatform.organization.domain.valueobject;

import java.util.UUID;

public record OrganizationId(UUID value) {
    public static OrganizationId of(UUID value) {
        return new OrganizationId(value);
    }
}
