package com.paymentplatform.payment.infrastructure.test;

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
    public void validateShop(long shopId) {
    }

    @Override
    public void validateSupplier(long supplierId) {
    }

    @Override
    public void validateRelation(long shopId, long supplierId) {
    }

    @Override
    public Optional<String> getOrganizationName(long organizationId) {
        return Optional.of("Org " + organizationId);
    }

    @Override
    public Optional<String> getUserName(long userId) {
        return Optional.of("User " + userId);
    }
}
