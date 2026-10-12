package com.paymentplatform.organization.application.usecase;

import com.paymentplatform.organization.application.dto.CreateOrganizationRequest;
import com.paymentplatform.organization.application.dto.UpdateOrganizationRequest;
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
 * Profil fiscal : régularisation MF/patente/TEJ et garde-fous SUSPENDU.
 * SpringBootTest, profil "test" (H2).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UpdateOrganizationFiscalProfileUseCaseH2Test {

    @Autowired private CreateOrganizationUseCase createOrg;
    @Autowired private UpdateOrganizationFiscalProfileUseCase fiscalProfile;

    private UUID actor() {
        return UUID.fromString("00000000-0000-0000-0000-000000000001");
    }

    private UUID createShop() {
        var response = createOrg.execute(new CreateOrganizationRequest("Shop Fisc", "SHOP",
                "1234567A/A/M/000", "PAT-TEST", "Adresse Test, Tunis", null, null, null, null, null, null),
                actor());
        return response.id();
    }

    private UpdateOrganizationRequest patch(String mf, String patente, String regime,
                                            String attestation, String tejId) {
        return new UpdateOrganizationRequest(null, mf, patente, null, regime, attestation, null, null, tejId, null);
    }

    @Test
    void update_setsTejIdentifierAndPatente() {
        UUID id = createShop();
        var updated = fiscalProfile.execute(id,
                patch("1234567A/A/M/000", "PAT-99", null, null, "TEJ-SHOP-1"), actor());
        assertThat(updated.patente()).isEqualTo("PAT-99");
        assertThat(updated.tejIdentifier()).isEqualTo("TEJ-SHOP-1");
    }

    @Test
    void update_suspendedWithoutAttestation_throws() {
        UUID id = createShop();
        assertThatThrownBy(() -> fiscalProfile.execute(id,
                patch("1234567A/A/M/000", null, "SUSPENDU", null, null), actor()))
                .isInstanceOf(com.paymentplatform.shared.domain.exception.DomainException.class)
                .hasMessageContaining("attestation");
    }

    @Test
    void update_suspendedWithAttestation_ok() {
        UUID id = createShop();
        var updated = fiscalProfile.execute(id,
                patch("1234567A/A/M/000", null, "SUSPENDU", "ATT-1", null), actor());
        assertThat(updated.fiscalRegime()).isEqualTo("SUSPENDU");
        assertThat(updated.suspensionAttestationNumber()).isEqualTo("ATT-1");
    }

    @Test
    void update_removingSupplierMatricule_throws() {
        var supplier = createOrg.execute(new CreateOrganizationRequest("Sup Fisc", "SUPPLIER",
                "1234567A/A/M/000", null, "Adresse Test, Tunis", null, null, null, null, null, null), actor());
        assertThatThrownBy(() -> fiscalProfile.execute(supplier.id(),
                patch("", null, null, null, null), actor()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("matricule fiscal");
    }
}
