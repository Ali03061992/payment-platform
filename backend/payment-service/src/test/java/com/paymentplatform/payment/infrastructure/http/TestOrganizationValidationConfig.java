package com.paymentplatform.payment.infrastructure.http;

import com.paymentplatform.shared.domain.exception.NotFoundException;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.Optional;
import java.util.UUID;

@TestConfiguration
public class TestOrganizationValidationConfig {

    @Bean
    @Primary
    public OrganizationValidationClient organizationValidationClient() {
        return new OrganizationValidationClient() {
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
                return Optional.of("Test Organization");
            }

            @Override
            public Optional<String> getUserName(UUID userId) {
                return Optional.of("Test User");
            }
        };
    }
}
