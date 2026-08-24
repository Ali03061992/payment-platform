package com.paymentplatform.identity.application.usecase;

import com.paymentplatform.shared.domain.exception.ConflictException;
import com.paymentplatform.shared.domain.model.OrganizationId;
import com.paymentplatform.shared.domain.model.RoleCode;
import com.paymentplatform.shared.domain.model.UserId;
import com.paymentplatform.shared.infrastructure.audit.AuditActions;
import com.paymentplatform.shared.infrastructure.audit.AuditRecorder;
import com.paymentplatform.shared.infrastructure.outbox.OutboxEventStore;
import com.paymentplatform.shared.domain.event.IdentityEvents.UserCreatedEvent;
import com.paymentplatform.identity.application.dto.CreateInternalUserRequest;
import com.paymentplatform.identity.application.dto.UserResponse;
import com.paymentplatform.identity.domain.model.User;
import com.paymentplatform.identity.domain.repository.UserRepository;
import com.paymentplatform.identity.domain.valueobject.Email;
import com.paymentplatform.identity.domain.valueobject.PasswordHash;
import com.paymentplatform.identity.domain.valueobject.PhoneNumber;
import com.paymentplatform.identity.domain.valueobject.Username;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Création d'utilisateurs pour le compte d'autres services (ex. admin initial
 * d'une organisation créée par le SYSTEM_ADMIN). Protégé par secret interne.
 */
@Service
public class InternalUserCreationUseCase {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuditRecorder audit;
    private final OutboxEventStore outbox;

    public InternalUserCreationUseCase(UserRepository users, PasswordEncoder passwordEncoder,
                                       AuditRecorder audit, OutboxEventStore outbox) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.audit = audit;
        this.outbox = outbox;
    }

    @Transactional
    public UserResponse createInternalUser(CreateInternalUserRequest request) {
        RoleCode role = RoleCode.from(request.role());
        Long orgId = request.organizationId();

        if (role == RoleCode.SYSTEM_ADMIN && orgId != null) {
            throw new ConflictException("Un SYSTEM_ADMIN ne peut pas être rattaché à une organisation");
        }
        if (role != RoleCode.SYSTEM_ADMIN && orgId == null) {
            throw new ConflictException("Une organisation est requise pour le rôle " + role);
        }

        Username username = Username.of(request.username());
        Email email = Email.of(request.email());
        if (users.existsByUsername(username)) {
            throw new ConflictException("Nom d'utilisateur déjà utilisé");
        }
        if (users.existsByEmail(email)) {
            throw new ConflictException("Adresse email déjà utilisée");
        }

        String rawPassword = (request.password() == null || request.password().isBlank())
                ? "test1234"
                : request.password();
        PasswordHash hash = PasswordHash.of(passwordEncoder.encode(rawPassword));

        User user = User.create(new UserId(0), username, email, hash, request.firstName(), request.lastName(),
                PhoneNumber.of(request.phone()), orgId == null ? null : OrganizationId.of(orgId), role);
        users.save(user);

        audit.record(null, orgId, AuditActions.USER_CREATED, user.id().value(),
                "{\"username\":\"" + user.username().value() + "\",\"role\":\"" + role + "\",\"by\":\"internal\"}");
        outbox.append(new UserCreatedEvent(UUID.randomUUID(), Instant.now(), user.id().value(), orgId,
                List.of(role.name())), String.valueOf(user.id().value()));

        return UserResponse.from(user);
    }
}