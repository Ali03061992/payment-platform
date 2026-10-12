package com.paymentplatform.organization.application.usecase;

import com.paymentplatform.organization.application.dto.CreateOrganizationRequest;
import com.paymentplatform.organization.application.dto.OrganizationResponse;
import com.paymentplatform.organization.domain.model.Organization;
import com.paymentplatform.organization.domain.repository.OrganizationRepository;
import com.paymentplatform.organization.domain.valueobject.OrganizationId;
import com.paymentplatform.organization.domain.valueobject.OrganizationName;
import com.paymentplatform.organization.domain.valueobject.OrganizationType;
import com.paymentplatform.shared.domain.event.OrganizationEvents.ShopCreatedEvent;
import com.paymentplatform.shared.domain.event.OrganizationEvents.SupplierCreatedEvent;
import com.paymentplatform.shared.domain.exception.ConflictException;
import com.paymentplatform.shared.infrastructure.audit.AuditActions;
import com.paymentplatform.shared.infrastructure.audit.AuditRecorder;
import com.paymentplatform.shared.infrastructure.outbox.OutboxEventStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class CreateOrganizationUseCase {

    private final OrganizationRepository organizations;
    private final AuditRecorder audit;
    private final OutboxEventStore outbox;

    public CreateOrganizationUseCase(OrganizationRepository organizations, AuditRecorder audit,
                                     OutboxEventStore outbox) {
        this.organizations = organizations;
        this.audit = audit;
        this.outbox = outbox;
    }

    @Transactional
    public OrganizationResponse execute(CreateOrganizationRequest request, UUID actorUserId) {
        OrganizationType type = OrganizationType.from(request.type());
        OrganizationName name = OrganizationName.of(request.name());

        if (organizations.existsByName(name.value())) {
            throw new ConflictException("Une organisation avec ce nom existe déjà : " + name.value());
        }

        com.paymentplatform.organization.domain.model.FiscalRegime regime =
                com.paymentplatform.organization.domain.model.FiscalRegime.from(request.fiscalRegime());
        // Loi : tout professionnel immatriculé dispose d'un MF (déclaration d'existence,
        // art. 56 IRPP/IS ; mention obligatoire art. 18 Code TVA). Le fournisseur, qui
        // émet les factures, y est toujours tenu ; l'acheteur aussi sauf particulier
        // (NON_ASSUJETTI) ou client étranger (EXPORT).
        if (type == OrganizationType.SUPPLIER) {
            if (request.matriculeFiscal() == null || request.matriculeFiscal().isBlank()) {
                throw new ConflictException("Le matricule fiscal est obligatoire pour un fournisseur "
                        + "(art. 18 du Code TVA : mention obligatoire sur chaque facture)");
            }
            if (request.address() == null || request.address().isBlank()) {
                throw new ConflictException("L'adresse est obligatoire pour un fournisseur "
                        + "(identification sur facture, art. 18 du Code TVA)");
            }
        } else if (regime != com.paymentplatform.organization.domain.model.FiscalRegime.NON_ASSUJETTI
                && regime != com.paymentplatform.organization.domain.model.FiscalRegime.EXPORT
                && (request.matriculeFiscal() == null || request.matriculeFiscal().isBlank())) {
            throw new ConflictException("Le matricule fiscal est obligatoire pour une boutique professionnelle "
                    + "(seuls les particuliers et clients export en sont dispensés)");
        }

        Organization org = Organization.create(new OrganizationId(null), name, type);
        org.updateFiscalProfile(
                request.matriculeFiscal(),
                request.patente(),
                request.address(),
                regime,
                request.suspensionAttestationNumber(),
                request.suspensionAttestationDate(),
                request.suspensionExpiryDate(),
                request.tejIdentifier());
        org.setDefaultWithholdingNature(request.defaultWithholdingNature());
        Organization saved = organizations.save(org);

        audit.record(null, saved.id().value(), type == OrganizationType.SUPPLIER
                        ? AuditActions.SUPPLIER_CREATED : AuditActions.SHOP_CREATED,
                saved.id().value(),
                "{\"name\":\"" + saved.name().value() + "\",\"type\":\"" + type + "\"}");

        if (type == OrganizationType.SUPPLIER) {
            outbox.append(new SupplierCreatedEvent(UUID.randomUUID(), Instant.now(),
                    saved.id().value(), saved.name().value()), String.valueOf(saved.id().value()));
        } else {
            outbox.append(new ShopCreatedEvent(UUID.randomUUID(), Instant.now(),
                    saved.id().value(), saved.name().value()), String.valueOf(saved.id().value()));
        }

        return OrganizationResponse.from(saved);
    }
}
