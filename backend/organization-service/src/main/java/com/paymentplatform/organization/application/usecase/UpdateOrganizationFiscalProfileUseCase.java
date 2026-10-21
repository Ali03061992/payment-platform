package com.paymentplatform.organization.application.usecase;

import com.paymentplatform.organization.application.dto.OrganizationResponse;
import com.paymentplatform.organization.application.dto.UpdateOrganizationRequest;
import com.paymentplatform.organization.domain.model.FiscalRegime;
import com.paymentplatform.organization.domain.model.Organization;
import com.paymentplatform.organization.domain.repository.OrganizationRepository;
import com.paymentplatform.organization.domain.valueobject.OrganizationId;
import com.paymentplatform.shared.domain.exception.ConflictException;
import com.paymentplatform.shared.domain.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Met à jour le profil fiscal d'une organisation existante (MF, patente,
 * adresse, régime, attestation de suspension). Permet la régularisation des
 * organisations créées avant l'obligation du matricule fiscal.
 */
@Service
public class UpdateOrganizationFiscalProfileUseCase {

    private final OrganizationRepository organizations;

    public UpdateOrganizationFiscalProfileUseCase(OrganizationRepository organizations) {
        this.organizations = organizations;
    }

    @Transactional
    public OrganizationResponse execute(UUID organizationId, UpdateOrganizationRequest request, UUID actorUserId) {
        Organization org = organizations.findById(new OrganizationId(organizationId))
                .orElseThrow(() -> new NotFoundException("Organisation non trouvée : " + organizationId));

        FiscalRegime regime = request.fiscalRegime() != null
                ? FiscalRegime.from(request.fiscalRegime())
                : org.fiscalRegime();

        String matricule = request.matriculeFiscal() != null ? request.matriculeFiscal() : org.matriculeFiscal();
        if (org.isSupplier() && (matricule == null || matricule.isBlank())) {
            throw new ConflictException("Le matricule fiscal est obligatoire pour un fournisseur "
                    + "(art. 18 du Code TVA)");
        }
        if (!org.isSupplier() && regime != FiscalRegime.NON_ASSUJETTI && regime != FiscalRegime.EXPORT
                && (matricule == null || matricule.isBlank())) {
            throw new ConflictException("Le matricule fiscal est obligatoire pour une boutique professionnelle "
                    + "(seuls les particuliers et clients export en sont dispensés)");
        }

        org.updateFiscalProfile(
                matricule,
                request.patente() != null ? request.patente() : org.patente(),
                request.address() != null ? request.address() : org.address(),
                regime,
                request.suspensionAttestationNumber() != null
                        ? request.suspensionAttestationNumber() : org.suspensionAttestationNumber(),
                request.suspensionAttestationDate() != null
                        ? request.suspensionAttestationDate() : org.suspensionAttestationDate(),
                request.suspensionExpiryDate() != null
                        ? request.suspensionExpiryDate() : org.suspensionExpiryDate(),
                request.tejIdentifier() != null ? request.tejIdentifier() : org.tejIdentifier());
        if (request.defaultWithholdingNature() != null) {
            org.setDefaultWithholdingNature(request.defaultWithholdingNature());
        }
        if (request.teifEnabled() != null || request.tejEnabled() != null) {
            org.updateFiscalFeatureToggles(
                    request.teifEnabled() != null ? request.teifEnabled() : org.teifEnabled(),
                    request.tejEnabled() != null ? request.tejEnabled() : org.tejEnabled());
        }

        return OrganizationResponse.from(organizations.save(org));
    }
}
