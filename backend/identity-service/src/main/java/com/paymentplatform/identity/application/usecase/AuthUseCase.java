package com.paymentplatform.identity.application.usecase;

import com.paymentplatform.shared.domain.exception.UnauthorizedException;
import com.paymentplatform.shared.infrastructure.audit.AuditActions;
import com.paymentplatform.shared.infrastructure.audit.AuditRecorder;
import com.paymentplatform.shared.infrastructure.security.AuthenticatedUser;
import com.paymentplatform.identity.application.dto.LoginRequest;
import com.paymentplatform.identity.application.dto.LoginResponse;
import com.paymentplatform.identity.application.dto.UserResponse;
import com.paymentplatform.identity.application.port.OrganizationStatusPort;
import com.paymentplatform.identity.application.port.TokenIssuer;
import com.paymentplatform.identity.domain.model.User;
import com.paymentplatform.identity.domain.repository.UserRepository;
import com.paymentplatform.identity.domain.valueobject.Username;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Authentification : vérifie identifiants, statut utilisateur ET organisation active. */
@Service
public class AuthUseCase {

    private final UserRepository users;
    private final OrganizationStatusPort organizationStatus;
    private final PasswordEncoder passwordEncoder;
    private final TokenIssuer tokenIssuer;
    private final AuditRecorder audit;

    public AuthUseCase(UserRepository users, OrganizationStatusPort organizationStatus,
                       PasswordEncoder passwordEncoder, TokenIssuer tokenIssuer, AuditRecorder audit) {
        this.users = users;
        this.organizationStatus = organizationStatus;
        this.passwordEncoder = passwordEncoder;
        this.tokenIssuer = tokenIssuer;
        this.audit = audit;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = users.findByUsername(Username.of(request.username()))
                .orElseThrow(() -> new UnauthorizedException("Identifiants invalides"));

        if (!passwordEncoder.matches(request.password(), user.password().value())) {
            throw new UnauthorizedException("Identifiants invalides");
        }
        if (!user.isActive()) {
            throw new UnauthorizedException("Compte désactivé");
        }
        if (user.organizationId() != null) {
            var org = organizationStatus.getOrganizationStatus(user.organizationId().value());
            if (!org.isActive()) {
                throw new UnauthorizedException("Organisation désactivée");
            }
        }

        audit.record(user.id().value(), user.organizationId() == null ? null : user.organizationId().value(),
                AuditActions.USER_LOGIN, user.id().value(), null);

        List<String> roles = user.roles().stream().map(Enum::name).toList();
        var authenticated = new AuthenticatedUser(user.id().value(), user.username().value(), roles,
                user.organizationId() == null ? null : user.organizationId().value());
        String token = tokenIssuer.issue(authenticated);
        return LoginResponse.of(token, tokenIssuer.expirationSeconds(), UserResponse.from(user));
    }
}