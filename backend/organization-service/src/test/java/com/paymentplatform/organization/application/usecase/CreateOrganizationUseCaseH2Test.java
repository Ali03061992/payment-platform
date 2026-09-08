package com.paymentplatform.organization.application.usecase;

import com.paymentplatform.organization.application.dto.CreateOrganizationRequest;
import com.paymentplatform.organization.domain.repository.OrganizationRepository;
import com.paymentplatform.shared.domain.exception.ConflictException;
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
class CreateOrganizationUseCaseH2Test {

    @Autowired private CreateOrganizationUseCase createOrg;
    @Autowired private OrganizationRepository organizations;

    @Test
    void execute_supplier_createsSupplier() {
        var request = new CreateOrganizationRequest("Fournisseur Alpha", "SUPPLIER");
        var response = createOrg.execute(request, 1L);

        assertThat(response.id()).isGreaterThan(0);
        assertThat(response.name()).isEqualTo("Fournisseur Alpha");
        assertThat(response.type()).isEqualTo("SUPPLIER");
        assertThat(response.status()).isEqualTo("ACTIVE");
    }

    @Test
    void execute_shop_createsShop() {
        var request = new CreateOrganizationRequest("Boutique Tunis", "SHOP");
        var response = createOrg.execute(request, 1L);

        assertThat(response.type()).isEqualTo("SHOP");
    }

    @Test
    void execute_duplicateName_throwsConflict() {
        var request1 = new CreateOrganizationRequest("Unique Name", "SUPPLIER");
        createOrg.execute(request1, 1L);

        var request2 = new CreateOrganizationRequest("Unique Name", "SHOP");
        assertThatThrownBy(() -> createOrg.execute(request2, 1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("existe déjà");
    }

    @Test
    void execute_nullType_throws() {
        var request = new CreateOrganizationRequest("No Type Org", null);
        assertThatThrownBy(() -> createOrg.execute(request, 1L))
                .isInstanceOf(Exception.class);
    }
}
