package com.paymentplatform.identity.application.usecase;

import com.paymentplatform.identity.application.dto.CreateInternalUserRequest;
import com.paymentplatform.identity.application.dto.UserResponse;
import com.paymentplatform.identity.domain.model.User;
import com.paymentplatform.identity.domain.repository.UserRepository;
import com.paymentplatform.identity.domain.valueobject.Email;
import com.paymentplatform.identity.domain.valueobject.PasswordHash;
import com.paymentplatform.identity.domain.valueobject.PhoneNumber;
import com.paymentplatform.identity.domain.valueobject.Username;
import com.paymentplatform.shared.domain.event.IdentityEvents.UserCreatedEvent;
import com.paymentplatform.shared.domain.exception.ConflictException;
import com.paymentplatform.shared.domain.model.OrganizationId;
import com.paymentplatform.shared.domain.model.RoleCode;
import com.paymentplatform.shared.domain.model.UserId;
import com.paymentplatform.shared.infrastructure.audit.AuditActions;
import com.paymentplatform.shared.infrastructure.audit.AuditRecorder;
import com.paymentplatform.shared.infrastructure.outbox.OutboxEventStore;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * Création d'utilisateurs par un administrateur ou un service interne
 * (ex. admin initial d'une organisation créée par le SYSTEM_ADMIN).
 *
 * <p>Aucun mot de passe n'est saisi : un secret provisoire aléatoire est stocké
 * puis un email d'invitation permet à l'utilisateur de définir son mot de passe.</p>
 */
@Service
public class InternalUserCreationUseCase {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final PasswordSetupUseCase passwordSetup;
    private final AuditRecorder audit;
    private final OutboxEventStore outbox;
    private final SecureRandom secureRandom = new SecureRandom();

    public InternalUserCreationUseCase(UserRepository users, PasswordEncoder passwordEncoder,
                                       PasswordSetupUseCase passwordSetup, AuditRecorder audit,
                                       OutboxEventStore outbox) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.passwordSetup = passwordSetup;
        this.audit = audit;
        this.outbox = outbox;
    }

    @Transactional
    public UserResponse createInternalUser(CreateInternalUserRequest request) {
        RoleCode role = RoleCode.from(request.role());
        UUID orgId = request.organizationId();

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

        byte[] provisionalSecret = new byte[32];
        secureRandom.nextBytes(provisionalSecret);
        String provisionalPassword = Base64.getUrlEncoder().withoutPadding().encodeToString(provisionalSecret);
        PasswordHash hash = PasswordHash.of(passwordEncoder.encode(provisionalPassword));

        User user = User.create(new UserId(null), username, email, hash, request.firstName(), request.lastName(),
                PhoneNumber.of(request.phone()), orgId == null ? null : OrganizationId.of(orgId), role);
        User userInDb = users.save(user);

        audit.record(null, orgId, AuditActions.USER_CREATED, userInDb.id().value(),
                "{\"username\":\"" + userInDb.username().value() + "\",\"role\":\"" + role
                        + "\",\"by\":\"admin\",\"invitation\":\"email\"}");
        outbox.append(new UserCreatedEvent(UUID.randomUUID(), Instant.now(), userInDb.id().value(), orgId,
                List.of(role.name())), String.valueOf(userInDb.id().value()));

        passwordSetup.initiateSetup(userInDb.username().value());

        return UserResponse.from(userInDb);
    }
}