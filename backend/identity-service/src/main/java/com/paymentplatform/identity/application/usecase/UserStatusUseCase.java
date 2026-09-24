package com.paymentplatform.identity.application.usecase;

import com.paymentplatform.identity.application.dto.UserResponse;
import com.paymentplatform.identity.domain.model.User;
import com.paymentplatform.identity.domain.repository.UserRepository;
import com.paymentplatform.shared.domain.event.IdentityEvents.UserActivatedEvent;
import com.paymentplatform.shared.domain.event.IdentityEvents.UserDisabledEvent;
import com.paymentplatform.shared.domain.exception.NotFoundException;
import com.paymentplatform.shared.domain.model.UserId;
import com.paymentplatform.shared.infrastructure.audit.AuditActions;
import com.paymentplatform.shared.infrastructure.audit.AuditRecorder;
import com.paymentplatform.shared.infrastructure.outbox.OutboxEventStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/** Activation/désactivation individuelle d'un utilisateur (SYSTEM_ADMIN). */
@Service
public class UserStatusUseCase {

    private final UserRepository users;
    private final RefreshTokenService refreshTokens;
    private final AuditRecorder audit;
    private final OutboxEventStore outbox;

    public UserStatusUseCase(UserRepository users, RefreshTokenService refreshTokens,
                             AuditRecorder audit, OutboxEventStore outbox) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.audit = audit;
        this.outbox = outbox;
    }

    @Transactional
    public UserResponse disableUser(UUID actorUserId, UUID userId) {
        User user = users.findById(UserId.of(userId))
                .orElseThrow(() -> new NotFoundException("Utilisateur introuvable"));
        user.disable();
        users.save(user);
        // M1 : désactivation => toutes les sessions sont révoquées immédiatement.
        refreshTokens.revokeAll(userId);
        UUID orgId = user.organizationId() == null ? null : user.organizationId().value();
        audit.record(actorUserId, orgId, AuditActions.USER_DISABLED, userId, "{\"by\":\"system-admin\"}");
        outbox.append(new UserDisabledEvent(UUID.randomUUID(), Instant.now(), userId, orgId, "system-admin"),
                String.valueOf(userId));
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse activateUser(UUID actorUserId, UUID userId) {
        User user = users.findById(UserId.of(userId))
                .orElseThrow(() -> new NotFoundException("Utilisateur introuvable"));
        user.activate();
        users.save(user);
        UUID orgId = user.organizationId() == null ? null : user.organizationId().value();
        audit.record(actorUserId, orgId, AuditActions.USER_ENABLED, userId, "{\"by\":\"system-admin\"}");
        outbox.append(new UserActivatedEvent(UUID.randomUUID(), Instant.now(), userId, orgId),
                String.valueOf(userId));
        return UserResponse.from(user);
    }
}