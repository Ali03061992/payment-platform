package com.paymentplatform.identity.application.usecase;

import com.paymentplatform.identity.application.dto.DevLoginRequest;
import com.paymentplatform.identity.application.dto.LoginResponse;
import com.paymentplatform.identity.application.port.OrganizationStatusPort;
import com.paymentplatform.identity.domain.model.User;
import com.paymentplatform.identity.domain.repository.UserRepository;
import com.paymentplatform.identity.domain.valueobject.Username;
import com.paymentplatform.shared.domain.exception.ForbiddenException;
import com.paymentplatform.shared.domain.exception.NotFoundException;
import com.paymentplatform.shared.domain.exception.UnauthorizedException;
import com.paymentplatform.shared.infrastructure.audit.AuditActions;
import com.paymentplatform.shared.infrastructure.audit.AuditRecorder;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

/**
 * Dev-login sans password, réservé hors prod.
 * Échec HARD au boot si {@code app.auth.dev-login-enabled=true} avec profil prod.
 */
@Service
public class DevLoginUseCase {

    private final UserRepository users;
    private final OrganizationStatusPort organizationStatus;
    private final AuthTokenService tokens;
    private final AuditRecorder audit;
    private final Environment environment;
    private final boolean enabled;

    public DevLoginUseCase(UserRepository users, OrganizationStatusPort organizationStatus,
                           AuthTokenService tokens, AuditRecorder audit,
                           Environment environment,
                           @Value("${app.auth.dev-login-enabled:false}") boolean enabled) {
        this.users = users;
        this.organizationStatus = organizationStatus;
        this.tokens = tokens;
        this.audit = audit;
        this.environment = environment;
        this.enabled = enabled;
    }

    @PostConstruct
    void validateNotProd() {
        if (enabled && Arrays.asList(environment.getActiveProfiles()).contains("prod")) {
            throw new IllegalStateException(
                    "app.auth.dev-login-enabled=true est interdit avec le profil prod");
        }
    }

    @Transactional
    public LoginResponse login(DevLoginRequest request) {
        if (!enabled || isProd()) {
            throw new ForbiddenException("Dev-login désactivé");
        }
        User user = users.findByUsername(Username.of(request.username()))
                .orElseThrow(() -> new NotFoundException("Utilisateur introuvable"));
        if (!user.isActive()) {
            throw new UnauthorizedException("Compte désactivé");
        }
        if (user.organizationId() != null) {
            var org = organizationStatus.getOrganizationStatus(user.organizationId().value());
            if (!org.isActive()) {
                throw new UnauthorizedException("Organisation désactivée");
            }
        }
        audit.record(user.id().value(),
                user.organizationId() == null ? null : user.organizationId().value(),
                AuditActions.USER_LOGIN, user.id().value(), "{\"by\":\"dev-login\"}");
        return tokens.issueTokens(user);
    }

    private boolean isProd() {
        return Arrays.asList(environment.getActiveProfiles()).contains("prod");
    }
}
