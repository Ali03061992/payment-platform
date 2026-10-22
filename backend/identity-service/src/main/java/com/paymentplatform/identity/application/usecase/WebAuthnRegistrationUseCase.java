package com.paymentplatform.identity.application.usecase;

import com.paymentplatform.identity.application.dto.WebAuthnCredentialResponse;
import com.paymentplatform.identity.domain.model.User;
import com.paymentplatform.identity.domain.model.WebAuthnChallenge;
import com.paymentplatform.identity.domain.model.WebAuthnChallengePurpose;
import com.paymentplatform.identity.domain.repository.UserRepository;
import com.paymentplatform.identity.domain.repository.WebAuthnChallengeRepository;
import com.paymentplatform.identity.domain.repository.WebAuthnCredentialRepository;
import com.paymentplatform.shared.domain.model.UserId;
import com.paymentplatform.identity.infrastructure.configuration.WebAuthnJsonCodec;
import com.paymentplatform.identity.infrastructure.configuration.WebAuthnProperties;
import com.paymentplatform.identity.infrastructure.webauthn.WebAuthnSupport;
import com.paymentplatform.shared.domain.exception.NotFoundException;
import com.paymentplatform.shared.domain.exception.UnprocessableEntityException;
import com.paymentplatform.shared.domain.exception.UnauthorizedException;
import com.paymentplatform.shared.infrastructure.audit.AuditActions;
import com.paymentplatform.shared.infrastructure.audit.AuditRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.webauthn.api.AttestationConveyancePreference;
import org.springframework.security.web.webauthn.api.AuthenticatorSelectionCriteria;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.CredentialRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Enregistrement de passkeys : émission des options de création (challenge
 * aléatoire à usage unique) puis vérification de l'attestation par webauthn4j
 * avant persistance de la clé publique. Requiert une session authentifiée.
 */
@Service
public class WebAuthnRegistrationUseCase {

    private static final Logger log = LoggerFactory.getLogger(WebAuthnRegistrationUseCase.class);
    private static final DateTimeFormatter LABEL_DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.systemDefault());

    private final UserRepository users;
    private final WebAuthnCredentialRepository credentials;
    private final WebAuthnChallengeRepository challenges;
    private final org.springframework.security.web.webauthn.management.UserCredentialRepository credentialStore;
    private final org.springframework.security.web.webauthn.management.WebAuthnRelyingPartyOperations relyingParty;
    private final org.springframework.security.web.webauthn.api.PublicKeyCredentialRpEntity relyingPartyEntity;
    private final WebAuthnProperties properties;
    private final WebAuthnJsonCodec codec;
    private final AuditRecorder audit;

    public WebAuthnRegistrationUseCase(UserRepository users,
                                       WebAuthnCredentialRepository credentials,
                                       WebAuthnChallengeRepository challenges,
                                       org.springframework.security.web.webauthn.management.UserCredentialRepository credentialStore,
                                       org.springframework.security.web.webauthn.management.WebAuthnRelyingPartyOperations relyingParty,
                                       org.springframework.security.web.webauthn.api.PublicKeyCredentialRpEntity relyingPartyEntity,
                                       WebAuthnProperties properties,
                                       WebAuthnJsonCodec codec,
                                       AuditRecorder audit) {
        this.users = users;
        this.credentials = credentials;
        this.challenges = challenges;
        this.credentialStore = credentialStore;
        this.relyingParty = relyingParty;
        this.relyingPartyEntity = relyingPartyEntity;
        this.properties = properties;
        this.codec = codec;
        this.audit = audit;
    }

    /** Émet les options de création et persiste le challenge éphémère. */
    @Transactional
    public org.springframework.security.web.webauthn.api.PublicKeyCredentialCreationOptions startRegistration(UUID userId) {
        User user = users.findById(UserId.of(userId))
                .orElseThrow(() -> new NotFoundException("Utilisateur introuvable"));

        List<org.springframework.security.web.webauthn.api.PublicKeyCredentialDescriptor> exclude =
                credentialStore.findByUserId(WebAuthnSupport.userHandle(user.id().value())).stream()
                        .map(record -> org.springframework.security.web.webauthn.api.PublicKeyCredentialDescriptor
                                .builder()
                                .id(record.getCredentialId())
                                .type(record.getCredentialType())
                                .transports(record.getTransports())
                                .build())
                        .toList();

        var options = org.springframework.security.web.webauthn.api.PublicKeyCredentialCreationOptions.builder()
                .rp(relyingPartyEntity)
                .user(org.springframework.security.web.webauthn.api.ImmutablePublicKeyCredentialUserEntity.builder()
                        .name(user.username().value())
                        .displayName((user.firstName() + " " + user.lastName()).trim())
                        .id(WebAuthnSupport.userHandle(user.id().value()))
                        .build())
                .challenge(Bytes.random())
                .pubKeyCredParams(List.of(
                        org.springframework.security.web.webauthn.api.PublicKeyCredentialParameters.ES256,
                        org.springframework.security.web.webauthn.api.PublicKeyCredentialParameters.RS256))
                .timeout(Duration.ofSeconds(properties.timeoutSeconds()))
                .excludeCredentials(exclude)
                .authenticatorSelection(AuthenticatorSelectionCriteria.builder()
                        .residentKey(org.springframework.security.web.webauthn.api.ResidentKeyRequirement.PREFERRED)
                        .userVerification(
                                org.springframework.security.web.webauthn.api.UserVerificationRequirement.PREFERRED)
                        .build())
                .attestation(AttestationConveyancePreference.NONE)
                .build();

        challenges.deleteExpired(Instant.now());
        challenges.save(WebAuthnChallenge.issue(userId, WebAuthnChallengePurpose.REGISTER,
                options.getChallenge().getBytes(), codec.write(options),
                Instant.now().plus(properties.challengeTtlSeconds(), ChronoUnit.SECONDS)));
        return options;
    }

    /** Vérifie l'attestation et enregistre la credential (clé publique uniquement). */
    @Transactional
    public WebAuthnCredentialResponse finishRegistration(
            UUID userId,
            org.springframework.security.web.webauthn.api.PublicKeyCredential<
                    org.springframework.security.web.webauthn.api.AuthenticatorAttestationResponse> credential,
            String label) {
        if (credential == null || credential.getResponse() == null) {
            throw new UnauthorizedException("Réponse WebAuthn manquante");
        }
        User user = users.findById(UserId.of(userId))
                .orElseThrow(() -> new NotFoundException("Utilisateur introuvable"));
        Bytes challenge = codec.challengeFromClientData(credential.getResponse().getClientDataJSON());
        WebAuthnChallenge stored = challenges.findValid(challenge.getBytes(), WebAuthnChallengePurpose.REGISTER)
                .orElseThrow(() -> new UnauthorizedException("Challenge WebAuthn invalide ou expiré"));
        if (stored.userId() == null || !stored.userId().equals(userId)) {
            throw new UnauthorizedException("Challenge WebAuthn invalide ou expiré");
        }
        if (!challenges.consumeIfValid(stored.id(), Instant.now())) {
            throw new UnauthorizedException("Challenge WebAuthn invalide ou expiré");
        }

        // Rejeu des options : seuls challenge + RP + user entrent dans la
        // vérification webauthn4j (le mixin Jackson ne sait pas désérialiser
        // les options, le JSON stocké sert d'audit).
        var creationOptions = org.springframework.security.web.webauthn.api.PublicKeyCredentialCreationOptions
                .builder()
                .rp(relyingPartyEntity)
                .user(org.springframework.security.web.webauthn.api.ImmutablePublicKeyCredentialUserEntity.builder()
                        .name(user.username().value())
                        .displayName((user.firstName() + " " + user.lastName()).trim())
                        .id(WebAuthnSupport.userHandle(userId))
                        .build())
                .challenge(new Bytes(stored.challenge()))
                .pubKeyCredParams(List.of(
                        org.springframework.security.web.webauthn.api.PublicKeyCredentialParameters.ES256,
                        org.springframework.security.web.webauthn.api.PublicKeyCredentialParameters.RS256))
                .authenticatorSelection(AuthenticatorSelectionCriteria.builder()
                        .residentKey(org.springframework.security.web.webauthn.api.ResidentKeyRequirement.PREFERRED)
                        .userVerification(
                                org.springframework.security.web.webauthn.api.UserVerificationRequirement.PREFERRED)
                        .build())
                .build();
        if (!creationOptions.getUser().getId().toBase64UrlString()
                .equals(WebAuthnSupport.userHandle(userId).toBase64UrlString())) {
            throw new UnauthorizedException("Challenge WebAuthn invalide ou expiré");
        }

        String effectiveLabel = (label == null || label.isBlank())
                ? "Passkey — " + LABEL_DATE.format(Instant.now())
                : label.trim();

        CredentialRecord record;
        try {
            record = relyingParty.registerCredential(
                    new org.springframework.security.web.webauthn.management.ImmutableRelyingPartyRegistrationRequest(
                            creationOptions,
                            new org.springframework.security.web.webauthn.management.RelyingPartyPublicKey(
                                    credential, effectiveLabel)));
        } catch (RuntimeException e) {
            log.warn("Enregistrement WebAuthn refusé pour l'utilisateur {} : {}", userId, e.getMessage());
            throw new UnprocessableEntityException("Vérification de la passkey échouée");
        }

        credentialStore.save(record);
        audit.record(userId, null, AuditActions.WEBAUTHN_CREDENTIAL_REGISTERED, null,
                record.getCredentialId().toBase64UrlString());
        return WebAuthnCredentialResponse.from(
                credentials.findByUserId(userId).stream()
                        .filter(summary -> summary.credentialIdBase64Url()
                                .equals(record.getCredentialId().toBase64UrlString()))
                        .findFirst()
                        .orElseThrow(() -> new NotFoundException("Passkey introuvable après enregistrement")));
    }
}
