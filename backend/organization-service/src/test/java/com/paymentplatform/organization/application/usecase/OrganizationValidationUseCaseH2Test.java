package com.paymentplatform.organization.application.usecase;

import com.paymentplatform.organization.application.dto.CreateOrganizationRequest;
import com.paymentplatform.shared.domain.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrganizationValidationUseCaseH2Test {

    @Autowired private OrganizationValidationUseCase validation;
    @Autowired private CreateOrganizationUseCase createOrg;

    private long orgId;

    @BeforeEach
    void setUp() {
        var org = createOrg.execute(new CreateOrganizationRequest("Valid Supplier", "SUPPLIER"), 1L);
        orgId = org.id();
    }

    @Test
    void validate_validOrg_returnsStatus() {
        var response = validation.validate(orgId);
        assertThat(response.id()).isEqualTo(orgId);
        assertThat(response.name()).isEqualTo("Valid Supplier");
        assertThat(response.type()).isEqualTo("SUPPLIER");
        assertThat(response.status()).isEqualTo("ACTIVE");
    }

    @Test
    void validate_unknownOrg_throwsNotFound() {
        assertThatThrownBy(() -> validation.validate(999L))
                .isInstanceOf(NotFoundException.class);
    }
}
