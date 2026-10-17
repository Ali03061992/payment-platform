package com.paymentplatform.identity.application.usecase;

import com.paymentplatform.identity.application.dto.WebAuthnCredentialResponse;
import com.paymentplatform.identity.domain.model.User;
import com.paymentplatform.identity.domain.repository.UserRepository;
import com.paymentplatform.identity.domain.valueobject.Email;
import com.paymentplatform.identity.domain.valueobject.PasswordHash;
import com.paymentplatform.identity.domain.valueobject.PhoneNumber;
import com.paymentplatform.identity.domain.valueobject.Username;
import com.paymentplatform.identity.infrastructure.webauthn.WebAuthnSupport;
import com.paymentplatform.identity.test.TestWebAuthnAuthenticator;
import com.paymentplatform.shared.domain.exception.NotFoundException;
import com.paymentplatform.shared.domain.exception.UnauthorizedException;
import com.paymentplatform.shared.domain.model.OrganizationId;
import com.paymentplatform.shared.domain.model.RoleCode;
import com.paymentplatform.shared.domain.model.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Flux WebAuthn complets sur H2 : enregistrement d'appareil (attestation
 * "none" générée par {@link TestWebAuthnAuthenticator}), connexion par
 * assertion ES256, gestion (liste/renommage/révocation) et garde-fous
 * (challenge rejoué, credential révoquée).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WebAuthnUseCaseH2Test {

    @Autowired
    private WebAuthnRegistrationUseCase registration;
    @Autowired
    private WebAuthnLoginUseCase login;
    @Autowired
    private WebAuthnCredentialManagementUseCase management;
    @Autowired
    private UserRepository users;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private com.paymentplatform.identity.infrastructure.test.TestOrganizationStatusPort organizationStatus;

    private User user;

    @BeforeEach
    void setUp() {
        // Le port de test est un singleton partagé : d'autres tests peuvent avoir
        // désactivé l'organisation, on la réactive pour ce cas.
        organizationStatus.setStatus(UUID.fromString("00000000-0000-0000-0000-000000000005"),
                "SHOP", "ACTIVE");
        user = users.save(User.create(new UserId(null), Username.of("passkey.user"),
                Email.of("passkey.user@example.com"),
                PasswordHash.of(passwordEncoder.encode("Secret@1")),
                "Nadia", "Ben Salah", new PhoneNumber(null),
                OrganizationId.of(UUID.fromString("00000000-0000-0000-0000-000000000005")),
                RoleCode.SHOP_AGENT));
    }

    private WebAuthnCredentialResponse register(TestWebAuthnAuthenticator authenticator) {
        var options = registration.startRegistration(user.id().value());
        var credential = authenticator.create(options);
        return registration.finishRegistration(user.id().value(), credential, "Mon téléphone");
    }

    @Test
    void register_start_emitsOptionsWithChallenge() {
        var options = registration.startRegistration(user.id().value());

        assertThat(options.getChallenge().getBytes()).isNotEmpty();
        assertThat(options.getRp().getId()).isEqualTo("localhost");
        assertThat(options.getUser().getId().toBase64UrlString())
                .isEqualTo(WebAuthnSupport.userHandle(user.id().value()).toBase64UrlString());
        assertThat(options.getPubKeyCredParams()).isNotEmpty();
    }

    @Test
    void register_finish_savesCredential() {
        var authenticator = new TestWebAuthnAuthenticator();
        var response = register(authenticator);

        assertThat(response.id()).isNotNull();
        assertThat(response.label()).isEqualTo("Mon téléphone");
        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(response.credentialId())
                .isEqualTo(new org.springframework.security.web.webauthn.api.Bytes(authenticator.credentialId())
                        .toBase64UrlString());
        assertThat(management.list(user.id().value())).hasSize(1);
    }

    @Test
    void register_finish_wrongChallenge_throws() {
        var options = registration.startRegistration(user.id().value());
        var authenticator = new TestWebAuthnAuthenticator();
        // Attestation générée avec un challenge différent (authenticator ne reçoit
        // pas les options émises) : la vérification doit échouer.
        var otherOptions = org.springframework.security.web.webauthn.api.PublicKeyCredentialCreationOptions
                .builder()
                .rp(options.getRp())
                .user(options.getUser())
                .challenge(org.springframework.security.web.webauthn.api.Bytes.random())
                .pubKeyCredParams(options.getPubKeyCredParams())
                .authenticatorSelection(options.getAuthenticatorSelection())
                .attestation(options.getAttestation())
                .build();
        var credential = authenticator.create(otherOptions);

        assertThatThrownBy(() -> registration.finishRegistration(
                user.id().value(), credential, "Hack"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void register_finish_replaySameChallenge_throws() {
        var authenticator = new TestWebAuthnAuthenticator();
        var options = registration.startRegistration(user.id().value());
        var credential = authenticator.create(options);
        registration.finishRegistration(user.id().value(), credential, "Phone");

        assertThatThrownBy(() -> registration.finishRegistration(
                user.id().value(), credential, "Phone"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void login_fullFlow_issuesTokens() {
        var authenticator = new TestWebAuthnAuthenticator();
        register(authenticator);

        var requestOptions = login.startLogin("passkey.user");
        assertThat(requestOptions.getRpId()).isEqualTo("localhost");
        assertThat(requestOptions.getAllowCredentials()).hasSize(1);

        var assertion = authenticator.get(requestOptions, WebAuthnSupport.userHandle(user.id().value()).getBytes());
        var response = login.finishLogin(assertion);

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.user().username()).isEqualTo("passkey.user");
        assertThat(response.refreshToken()).isNotBlank();
    }

    @Test
    void login_unknownUsername_issuesDiscoverableOptions() {
        var requestOptions = login.startLogin("ghost.user");

        assertThat(requestOptions.getAllowCredentials()).isEmpty();
    }

    @Test
    void login_revokedCredential_throws() {
        var authenticator = new TestWebAuthnAuthenticator();
        var saved = register(authenticator);
        management.revoke(user.id().value(), saved.id());

        var requestOptions = login.startLogin("passkey.user");
        assertThat(requestOptions.getAllowCredentials()).isEmpty();

        var assertion = authenticator.get(requestOptions, WebAuthnSupport.userHandle(user.id().value()).getBytes());
        assertThatThrownBy(() -> login.finishLogin(assertion))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void management_rename_updatesLabel() {
        var authenticator = new TestWebAuthnAuthenticator();
        var saved = register(authenticator);

        management.rename(user.id().value(), saved.id(), "Bureau");

        List<WebAuthnCredentialResponse> list = management.list(user.id().value());
        assertThat(list).hasSize(1);
        assertThat(list.get(0).label()).isEqualTo("Bureau");
    }

    @Test
    void management_revoke_unknownCredential_throws() {
        assertThatThrownBy(() -> management.revoke(user.id().value(), UUID.randomUUID()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void management_rename_otherUserCredential_throws() {
        var authenticator = new TestWebAuthnAuthenticator();
        var saved = register(authenticator);

        User other = users.save(User.create(new UserId(null), Username.of("other.user"),
                Email.of("other.user@example.com"),
                PasswordHash.of(passwordEncoder.encode("Secret@1")),
                "Autre", "Utilisateur", new PhoneNumber(null),
                OrganizationId.of(UUID.fromString("00000000-0000-0000-0000-000000000005")),
                RoleCode.SHOP_AGENT));

        assertThatThrownBy(() -> management.rename(other.id().value(), saved.id(), "Voleur"))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> management.revoke(other.id().value(), saved.id()))
                .isInstanceOf(NotFoundException.class);
    }
}
