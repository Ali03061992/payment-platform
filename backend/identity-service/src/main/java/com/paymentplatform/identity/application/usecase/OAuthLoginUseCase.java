package com.paymentplatform.identity.application.usecase;

import com.paymentplatform.identity.application.dto.LoginResponse;
import com.paymentplatform.identity.application.dto.OAuthLoginRequest;
import com.paymentplatform.identity.application.port.OrganizationStatusPort;
import com.paymentplatform.identity.domain.model.AuthProvider;
import com.paymentplatform.identity.domain.model.User;
import com.paymentplatform.identity.domain.repository.UserRepository;
import com.paymentplatform.identity.domain.valueobject.Email;
import com.paymentplatform.identity.infrastructure.security.OidcTokenVerifier;
import com.paymentplatform.shared.domain.exception.ForbiddenException;
import com.paymentplatform.shared.domain.exception.UnauthorizedException;
import com.paymentplatform.shared.infrastructure.audit.AuditActions;
import com.paymentplatform.shared.infrastructure.audit.AuditRecorder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Login OAuth passwordless : vérifie l'idToken OIDC puis résout le compte
 * EXISTANT par email vérifié — rôle et organisation sont hérités de ce compte.
 * Aucune création automatique : un email inconnu donne 401 (le compte doit
 * être créé/validé par un administrateur au préalable).
 */
@Service
public class OAuthLoginUseCase {

    private final OidcTokenVerifier verifier;
    private final UserRepository users;
    private final OrganizationStatusPort organizationStatus;
    private final AuthTokenService tokens;
    private final AuditRecorder audit;

    public OAuthLoginUseCase(OidcTokenVerifier verifier, UserRepository users,
                             OrganizationStatusPort organizationStatus,
                             AuthTokenService tokens, AuditRecorder audit) {
        this.verifier = verifier;
        this.users = users;
        this.organizationStatus = organizationStatus;
        this.tokens = tokens;
        this.audit = audit;
    }

    @Transactional
    public LoginResponse login(OAuthLoginRequest request) {
        var verified = verifier.verify(request.provider(), request.idToken());

        if (!verified.emailVerified()) {
            throw new UnauthorizedException("Email non vérifié par le fournisseur OAuth");
        }
        Email email = Email.of(verified.email());
        AuthProvider provider = toProvider(request.provider());

        var existing = users.findByEmail(email);
        if (existing.isPresent()) {
            User user = existing.get();
            if (!user.isActive()) {
                throw new ForbiddenException("Compte en attente de validation");
            }
            if (user.providerSubject() != null && !user.providerSubject().equals(verified.subject())) {
                // Subject différent déjà lié : possible usurpation — refuse.
                if (user.authProvider() == provider) {
                    throw new UnauthorizedException("Compte OAuth déjà lié à un autre subject");
                }
            }
            user.linkOAuth(provider, verified.subject(), true);
            users.save(user);

            if (user.organizationId() != null) {
                var org = organizationStatus.getOrganizationStatus(user.organizationId().value());
                if (!org.isActive()) {
                    throw new UnauthorizedException("Organisation désactivée");
                }
            }

            audit.record(user.id().value(),
                    user.organizationId() == null ? null : user.organizationId().value(),
                    AuditActions.USER_LOGIN, user.id().value(),
                    "{\"by\":\"oauth\",\"provider\":\"" + provider + "\"}");
            return tokens.issueTokens(user);
        }

        // Aucun compte associé : pas de création automatique (décision :
        // l'email doit pré-exister dans la liste des utilisateurs, le rôle et
        // l'organisation sont hérités de ce compte).
        throw new UnauthorizedException(
                "Aucun compte associé à cet email — contactez un administrateur");
    }

    private AuthProvider toProvider(String raw) {
        try {
            AuthProvider p = AuthProvider.from(raw);
            if (p == AuthProvider.LOCAL) {
                throw new UnauthorizedException("Fournisseur OAuth inconnu : " + raw);
            }
            return p;
        } catch (IllegalArgumentException e) {
            throw new UnauthorizedException("Fournisseur OAuth inconnu : " + raw);
        }
    }
}
