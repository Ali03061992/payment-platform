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

        Organization org = Organization.create(new OrganizationId(null), name, type);
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
