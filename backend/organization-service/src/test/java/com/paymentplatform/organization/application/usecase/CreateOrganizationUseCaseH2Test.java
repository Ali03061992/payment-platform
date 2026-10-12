package com.paymentplatform.organization.application.usecase;

import com.paymentplatform.organization.application.dto.CreateOrganizationRequest;
import com.paymentplatform.organization.domain.repository.OrganizationRepository;
import com.paymentplatform.shared.domain.exception.ConflictException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
/**
 * Tests de CreateOrganizationUseCaseH2Test.
 * Perimetre : cas d'usage/service CreateOrganizationUseCase sur base H2.
 * Moyens : contexte SpringBootTest, profil "test" (H2).
 */

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CreateOrganizationUseCaseH2Test {

    @Autowired private CreateOrganizationUseCase createOrg;
    @Autowired private OrganizationRepository organizations;

    private static CreateOrganizationRequest org(String name, String type) {
        return new CreateOrganizationRequest(name, type, "1234567A/A/M/000", "PAT-TEST",
                "Adresse Test, Tunis", null, null, null, null, null, null);
    }

    @Test
    void execute_supplier_createsSupplier() {
        var response = createOrg.execute(org("Fournisseur Alpha", "SUPPLIER"),
                UUID.fromString("00000000-0000-0000-0000-000000000001"));

        assertThat(response.id()).isNotNull();
        assertThat(response.name()).isEqualTo("Fournisseur Alpha");
        assertThat(response.type()).isEqualTo("SUPPLIER");
        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(response.matriculeFiscal()).isEqualTo("1234567A/A/M/000");
    }

    @Test
    void execute_shop_createsShop() {
        var response = createOrg.execute(org("Boutique Tunis", "SHOP"),
                UUID.fromString("00000000-0000-0000-0000-000000000001"));

        assertThat(response.type()).isEqualTo("SHOP");
    }

    @Test
    void execute_duplicateName_throwsConflict() {
        createOrg.execute(org("Unique Name", "SUPPLIER"),
                UUID.fromString("00000000-0000-0000-0000-000000000001"));

        var request2 = org("Unique Name", "SHOP");
        assertThatThrownBy(() -> createOrg.execute(request2, UUID.fromString("00000000-0000-0000-0000-000000000001")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("existe déjà");
    }

    @Test
    void execute_nullType_throws() {
        var request = new CreateOrganizationRequest("No Type Org", null);
        assertThatThrownBy(() -> createOrg.execute(request, UUID.fromString("00000000-0000-0000-0000-000000000001")))
                .isInstanceOf(Exception.class);
    }

    @Test
    void execute_supplierWithoutMatricule_throws() {
        var request = new CreateOrganizationRequest("Sans MF", "SUPPLIER", null, null,
                "Adresse Test, Tunis", null, null, null, null, null, null);
        assertThatThrownBy(() -> createOrg.execute(request, UUID.fromString("00000000-0000-0000-0000-000000000001")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("matricule fiscal");
    }

    @Test
    void execute_supplierWithoutAddress_throws() {
        var request = new CreateOrganizationRequest("Sans Adresse", "SUPPLIER", "1234567A/A/M/000", null,
                null, null, null, null, null, null, null);
        assertThatThrownBy(() -> createOrg.execute(request, UUID.fromString("00000000-0000-0000-0000-000000000001")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("adresse");
    }

    @Test
    void execute_particulierWithoutMatricule_ok() {
        var request = new CreateOrganizationRequest("Particulier", "SHOP", null, null,
                null, "NON_ASSUJETTI", null, null, null, null, null);
        var response = createOrg.execute(request, UUID.fromString("00000000-0000-0000-0000-000000000001"));
        assertThat(response.fiscalRegime()).isEqualTo("NON_ASSUJETTI");
        assertThat(response.matriculeFiscal()).isNull();
    }

    @Test
    void execute_proShopWithoutMatricule_throws() {
        var request = new CreateOrganizationRequest("Pro sans MF", "SHOP");
        assertThatThrownBy(() -> createOrg.execute(request, UUID.fromString("00000000-0000-0000-0000-000000000001")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("matricule fiscal");
    }
}
