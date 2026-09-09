package com.paymentplatform.organization.application.usecase;

import com.paymentplatform.shared.domain.exception.ConflictException;
import com.paymentplatform.shared.domain.exception.NotFoundException;
import com.paymentplatform.shared.infrastructure.audit.AuditActions;
import com.paymentplatform.shared.infrastructure.audit.AuditRecorder;
import com.paymentplatform.shared.infrastructure.outbox.OutboxEventStore;
import com.paymentplatform.shared.domain.event.OrganizationEvents.*;
import com.paymentplatform.organization.application.dto.OrganizationResponse;
import com.paymentplatform.organization.domain.model.Organization;
import com.paymentplatform.organization.domain.repository.OrganizationRepository;
import com.paymentplatform.organization.domain.repository.SupplierShopRelationRepository;
import com.paymentplatform.organization.domain.valueobject.OrganizationId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class OrganizationStatusUseCase {

    private final OrganizationRepository organizations;
    private final SupplierShopRelationRepository relations;
    private final AuditRecorder audit;
    private final OutboxEventStore outbox;

    public OrganizationStatusUseCase(OrganizationRepository organizations,
                                     SupplierShopRelationRepository relations,
                                     AuditRecorder audit, OutboxEventStore outbox) {
        this.organizations = organizations;
        this.relations = relations;
        this.audit = audit;
        this.outbox = outbox;
    }

    @Transactional
    public OrganizationResponse activate(UUID id, UUID actorUserId) {
        Organization org = organizations.findById(OrganizationId.of(id))
                .orElseThrow(() -> new NotFoundException("Organisation non trouvée : " + id));

        org.activate();
        Organization saved = organizations.save(org);

        String action = org.isSupplier() ? AuditActions.SUPPLIER_ENABLED : AuditActions.SHOP_ENABLED;
        audit.record(null, id, action, id, "{\"name\":\"" + saved.name().value() + "\"}");

        if (org.isSupplier()) {
            outbox.append(new SupplierActivatedEvent(UUID.randomUUID(), Instant.now(), id),
                    String.valueOf(id));
        } else {
            outbox.append(new ShopActivatedEvent(UUID.randomUUID(), Instant.now(), id),
                    String.valueOf(id));
        }

        return OrganizationResponse.from(saved);
    }

    @Transactional
    public OrganizationResponse disable(UUID id, UUID actorUserId) {
        Organization org = organizations.findById(OrganizationId.of(id))
                .orElseThrow(() -> new NotFoundException("Organisation non trouvée : " + id));

        org.disable();
        Organization saved = organizations.save(org);

        relations.deactivateBySupplierId(OrganizationId.of(id));
        relations.deactivateByShopId(OrganizationId.of(id));

        String action = org.isSupplier() ? AuditActions.SUPPLIER_DISABLED : AuditActions.SHOP_DISABLED;
        audit.record(null, id, action, id, "{\"name\":\"" + saved.name().value() + "\"}");

        if (org.isSupplier()) {
            outbox.append(new SupplierDisabledEvent(UUID.randomUUID(), Instant.now(), id),
                    String.valueOf(id));
        } else {
            outbox.append(new ShopDisabledEvent(UUID.randomUUID(), Instant.now(), id),
                    String.valueOf(id));
        }

        return OrganizationResponse.from(saved);
    }
}
