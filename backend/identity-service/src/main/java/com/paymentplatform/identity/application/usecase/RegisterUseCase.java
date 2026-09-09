package com.paymentplatform.identity.application.usecase;

import com.paymentplatform.shared.domain.exception.ConflictException;
import com.paymentplatform.shared.domain.model.RoleCode;
import com.paymentplatform.shared.domain.model.UserId;
import com.paymentplatform.shared.infrastructure.audit.AuditActions;
import com.paymentplatform.shared.infrastructure.audit.AuditRecorder;
import com.paymentplatform.shared.infrastructure.outbox.OutboxEventStore;
import com.paymentplatform.shared.domain.event.IdentityEvents.UserCreatedEvent;
import com.paymentplatform.identity.application.dto.RegisterRequest;
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
import java.util.Set;
import java.util.UUID;

/** Inscription publique : crée un compte utilisateur (sans organisation). */
@Service
public class RegisterUseCase {

    private static final Set<RoleCode> ALLOWED_ROLES = Set.of(
            RoleCode.SUPPLIER_ADMIN,
            RoleCode.SUPPLIER_AGENT,
            RoleCode.SHOP_ADMIN,
            RoleCode.SHOP_AGENT
    );

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuditRecorder audit;
    private final OutboxEventStore outbox;

    public RegisterUseCase(UserRepository users, PasswordEncoder passwordEncoder,
                           AuditRecorder audit, OutboxEventStore outbox) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.audit = audit;
        this.outbox = outbox;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        RoleCode role = RoleCode.from(request.role());

        if (!ALLOWED_ROLES.contains(role)) {
            throw new ConflictException("Rôle non autorisé pour l'inscription : " + request.role()
                    + ". Rôles autorisés : SUPPLIER_ADMIN, SUPPLIER_AGENT, SHOP_ADMIN, SHOP_AGENT");
        }

        Username username = Username.of(request.username());
        Email email = Email.of(request.email());
        if (users.existsByUsername(username)) {
            throw new ConflictException("Nom d'utilisateur déjà utilisé");
        }
        if (users.existsByEmail(email)) {
            throw new ConflictException("Adresse email déjà utilisée");
        }

        PasswordHash hash = PasswordHash.of(passwordEncoder.encode(request.password()));
        User user = User.create(new UserId(null), username, email, hash,
                request.firstName(), request.lastName(),
                PhoneNumber.of(request.phone()), null, role);
        users.save(user);

        audit.record(null, null, AuditActions.USER_CREATED, user.id().value(),
                "{\"username\":\"" + user.username().value() + "\",\"role\":\"" + role + "\",\"by\":\"self-register\"}");
        outbox.append(new UserCreatedEvent(UUID.randomUUID(), Instant.now(), user.id().value(), null,
                List.of(role.name())), String.valueOf(user.id().value()));

        return UserResponse.from(user);
    }
}
