package com.paymentplatform.identity.application.usecase;

import com.paymentplatform.identity.application.dto.LoginResponse;
import com.paymentplatform.identity.application.dto.UserResponse;
import com.paymentplatform.identity.application.port.OrganizationStatusPort;
import com.paymentplatform.identity.application.port.TokenIssuer;
import com.paymentplatform.identity.domain.model.User;
import com.paymentplatform.identity.domain.model.WebAuthnChallenge;
import com.paymentplatform.identity.domain.model.WebAuthnChallengePurpose;
import com.paymentplatform.identity.domain.repository.UserRepository;
import com.paymentplatform.identity.domain.repository.WebAuthnChallengeRepository;
import com.paymentplatform.identity.domain.repository.WebAuthnCredentialRepository;
import com.paymentplatform.identity.infrastructure.configuration.WebAuthnJsonCodec;
import com.paymentplatform.identity.infrastructure.configuration.WebAuthnProperties;
import com.paymentplatform.identity.infrastructure.webauthn.WebAuthnSupport;
import com.paymentplatform.shared.domain.exception.UnauthorizedException;
import com.paymentplatform.shared.domain.model.UserId;
import com.paymentplatform.shared.infrastructure.audit.AuditActions;
import com.paymentplatform.shared.infrastructure.audit.AuditRecorder;
import com.paymentplatform.shared.infrastructure.security.AuthenticatedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.webauthn.api.AuthenticatorAssertionResponse;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.PublicKeyCredential;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialDescriptor;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRequestOptions;
import org.springframework.security.web.webauthn.api.UserVerificationRequirement;
import org.springframework.security.web.webauthn.management.RelyingPartyAuthenticationRequest;
import org.springframework.security.web.webauthn.management.WebAuthnRelyingPartyOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Connexion par passkey : émission des options d'assertion (challenge aléatoire
 * à usage unique) puis vérification de la signature, de l'origine et du RP ID
 * par webauthn4j. Émet les mêmes tokens JWT/refresh que la connexion par mot de
 * passe. Un utilisateur inconnu reçoit des options sans allowCredentials (flux
 * discoverable) afin de ne pas révéler l'existence du compte.
 */
@Service
public class WebAuthnLoginUseCase {

    private static final Logger log = LoggerFactory.getLogger(WebAuthnLoginUseCase.class);

    private final UserRepository users;
    private final WebAuthnChallengeRepository challenges;
    private final WebAuthnCredentialRepository credentials;
    private final org.springframework.security.web.webauthn.management.UserCredentialRepository credentialStore;
    private final WebAuthnRelyingPartyOperations relyingParty;
    private final OrganizationStatusPort organizationStatus;
    private final TokenIssuer tokenIssuer;
    private final RefreshTokenService refreshTokens;
    private final WebAuthnProperties properties;
    private final WebAuthnJsonCodec codec;
    private final AuditRecorder audit;

    public WebAuthnLoginUseCase(UserRepository users,
                                WebAuthnChallengeRepository challenges,
                                WebAuthnCredentialRepository credentials,
                                org.springframework.security.web.webauthn.management.UserCredentialRepository credentialStore,
                                WebAuthnRelyingPartyOperations relyingParty,
                                OrganizationStatusPort organizationStatus,
                                TokenIssuer tokenIssuer,
                                RefreshTokenService refreshTokens,
                                WebAuthnProperties properties,
                                WebAuthnJsonCodec codec,
                                AuditRecorder audit) {
        this.users = users;
        this.challenges = challenges;
        this.credentials = credentials;
        this.credentialStore = credentialStore;
        this.relyingParty = relyingParty;
        this.organizationStatus = organizationStatus;
        this.tokenIssuer = tokenIssuer;
        this.refreshTokens = refreshTokens;
        this.properties = properties;
        this.codec = codec;
        this.audit = audit;
    }

    /** Émet les options d'assertion (username-first ou discoverable). */
    @Transactional
    public PublicKeyCredentialRequestOptions startLogin(String username) {
        UUID userId = null;
        List<PublicKeyCredentialDescriptor> allowCredentials = List.of();
        if (username != null && !username.isBlank()) {
            userId = users.findByUsername(
                            com.paymentplatform.identity.domain.valueobject.Username.of(username.trim()))
                    .map(user -> user.id().value())
                    .orElse(null);
            if (userId != null) {
                allowCredentials = credentialStore.findByUserId(WebAuthnSupport.userHandle(userId)).stream()
                        .map(record -> PublicKeyCredentialDescriptor.builder()
                                .id(record.getCredentialId())
                                .type(record.getCredentialType())
                                .transports(record.getTransports())
                                .build())
                        .toList();
            }
        }

        var options = PublicKeyCredentialRequestOptions.builder()
                .challenge(Bytes.random())
                .timeout(Duration.ofSeconds(properties.timeoutSeconds()))
                .rpId(properties.rpId())
                .allowCredentials(allowCredentials)
                .userVerification(UserVerificationRequirement.PREFERRED)
                .build();

        challenges.deleteExpired(Instant.now());
        challenges.save(WebAuthnChallenge.issue(userId, WebAuthnChallengePurpose.AUTHENTICATE,
                options.getChallenge().getBytes(), codec.write(options),
                Instant.now().plus(properties.challengeTtlSeconds(), ChronoUnit.SECONDS)));
        return options;
    }

    /** Vérifie l'assertion et émet le couple access/refresh tokens. */
    @Transactional
    public LoginResponse finishLogin(PublicKeyCredential<AuthenticatorAssertionResponse> credential) {
        if (credential == null || credential.getResponse() == null) {
            throw new UnauthorizedException("Authentification passkey échouée");
        }
        Bytes challenge = codec.challengeFromClientData(credential.getResponse().getClientDataJSON());
        WebAuthnChallenge stored = challenges.findValid(challenge.getBytes(), WebAuthnChallengePurpose.AUTHENTICATE)
                .orElseThrow(() -> new UnauthorizedException("Challenge WebAuthn invalide ou expiré"));
        if (!challenges.consumeIfValid(stored.id(), Instant.now())) {
            throw new UnauthorizedException("Challenge WebAuthn invalide ou expiré");
        }

        // Rejeu des options : challenge + rpId + userVerification entrent dans
        // la vérification webauthn4j (le JSON stocké sert d'audit).
        var requestOptions = PublicKeyCredentialRequestOptions.builder()
                .challenge(new Bytes(stored.challenge()))
                .rpId(properties.rpId())
                .allowCredentials(List.of())
                .userVerification(UserVerificationRequirement.PREFERRED)
                .build();
        org.springframework.security.web.webauthn.api.PublicKeyCredentialUserEntity userEntity;
        try {
            userEntity = relyingParty.authenticate(
                    new RelyingPartyAuthenticationRequest(requestOptions, credential));
        } catch (RuntimeException e) {
            log.warn("Assertion WebAuthn refusée : {}", e.getMessage());
            throw new UnauthorizedException("Authentification passkey échouée");
        }

        UUID userId = WebAuthnSupport.userIdFromHandle(userEntity.getId());
        if (stored.userId() != null && !stored.userId().equals(userId)) {
            throw new UnauthorizedException("Authentification passkey échouée");
        }

        User user = users.findById(UserId.of(userId))
                .orElseThrow(() -> new UnauthorizedException("Authentification passkey échouée"));
        if (!user.isActive()) {
            throw new UnauthorizedException("Compte désactivé");
        }
        if (user.organizationId() != null) {
            var org = organizationStatus.getOrganizationStatus(user.organizationId().value());
            if (!org.isActive()) {
                throw new UnauthorizedException("Organisation désactivée");
            }
        }

        long signatureCount = WebAuthnSupport.parseSignatureCount(
                credential.getResponse().getAuthenticatorData().getBytes());
        credentials.touchByCredentialId(credential.getRawId().getBytes(), signatureCount);

        audit.record(user.id().value(),
                user.organizationId() == null ? null : user.organizationId().value(),
                AuditActions.WEBAUTHN_LOGIN, user.id().value(), null);

        List<String> roles = user.roles().stream().map(Enum::name).toList();
        var authenticated = new AuthenticatedUser(user.id().value(), user.username().value(), roles,
                user.organizationId() == null ? null : user.organizationId().value());
        String token = tokenIssuer.issue(authenticated);
        var refresh = refreshTokens.issue(user.id().value());
        return LoginResponse.of(token, tokenIssuer.expirationSeconds(), UserResponse.from(user),
                refresh.rawToken(), refresh.expiresInSeconds());
    }
}
