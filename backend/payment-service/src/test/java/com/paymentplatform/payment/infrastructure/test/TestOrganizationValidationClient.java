package com.paymentplatform.payment.infrastructure.test;

import java.util.UUID;

import com.paymentplatform.payment.infrastructure.http.OrganizationValidationClient;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Primary
@Profile("test")
public class TestOrganizationValidationClient extends OrganizationValidationClient {

    @Override
    public void validateShop(UUID shopId) {
    }

    @Override
    public void validateSupplier(UUID supplierId) {
    }

    @Override
    public void validateRelation(UUID shopId, UUID supplierId) {
    }

    @Override
    public Optional<String> getOrganizationName(UUID organizationId) {
        return Optional.of("Org " + organizationId);
    }

    @Override
    public Optional<String> getUserName(UUID userId) {
        return Optional.of("User " + userId);
    }
}
