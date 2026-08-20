package com.paymentplatform.identity.application.usecase;

import com.paymentplatform.shared.domain.exception.DomainException;
import com.paymentplatform.shared.domain.model.OrganizationId;
import com.paymentplatform.shared.domain.model.RoleCode;
import com.paymentplatform.shared.infrastructure.audit.AuditActions;
import com.paymentplatform.shared.infrastructure.audit.AuditRecorder;
import com.paymentplatform.shared.infrastructure.eventing.EventDeduplicator;
import com.paymentplatform.shared.infrastructure.outbox.OutboxEventStore;
import com.paymentplatform.shared.domain.event.IdentityEvents.UserDisabledEvent;
import com.paymentplatform.identity.domain.model.User;
import com.paymentplatform.identity.domain.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Cascade de désactivation : réception de SupplierDisabledEvent / ShopDisabledEvent
 * ⇒ désactivation de tous les comptes rattachés à l'organisation.
 * Idempotent : un utilisateur déjà DISABLED n'est pas modifié.
 */
@Service
public class OrganizationCascadeUseCase {

    private static final Logger log = LoggerFactory.getLogger(OrganizationCascadeUseCase.class);

    private final UserRepository users;
    private final AuditRecorder audit;
    private final OutboxEventStore outbox;
    private final EventDeduplicator deduplicator;

    public OrganizationCascadeUseCase(UserRepository users, AuditRecorder audit, OutboxEventStore outbox,
                                      EventDeduplicator deduplicator) {
        this.users = users;
        this.audit = audit;
        this.outbox = outbox;
        this.deduplicator = deduplicator;
    }

    @Transactional
    public void onOrganizationDisabled(String eventType, long organizationId, List<RoleCode> targetRoles,
                                       String eventId) {
        if (deduplicator.isProcessed(eventId)) {
            log.debug("Événement {} déjà traité", eventId);
            return;
        }
        List<User> members = users.findByOrganizationId(OrganizationId.of(organizationId)).stream()
                .filter(u -> u.roles().stream().anyMatch(targetRoles::contains))
                .toList();

        int disabled = 0;
        for (User user : members) {
            if (user.isActive()) {
                user.disable();
                users.save(user);
                audit.record(null, organizationId, AuditActions.USER_DISABLED, user.id().value(),
                        "{\"by\":\"org-cascade\",\"event\":" + eventType + "}");
                outbox.append(new UserDisabledEvent(UUID.randomUUID(), Instant.now(), user.id().value(),
                        organizationId, "org-cascade"), String.valueOf(user.id().value()));
                disabled++;
            }
        }
        log.info("Cascade {} sur organisation {} : {} compte(s) désactivé(s)", eventType, organizationId, disabled);
        deduplicator.markProcessed(eventId);
    }
}