package com.paymentplatform.identity.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.paymentplatform.identity.application.dto.OAuthLoginRequest;
import com.paymentplatform.identity.domain.model.User;
import com.paymentplatform.identity.domain.repository.UserRepository;
import com.paymentplatform.identity.domain.valueobject.Email;
import com.paymentplatform.identity.domain.valueobject.PasswordHash;
import com.paymentplatform.identity.domain.valueobject.PhoneNumber;
import com.paymentplatform.identity.domain.valueobject.Username;
import com.paymentplatform.identity.infrastructure.security.OidcProperties;
import com.paymentplatform.identity.infrastructure.security.OidcTokenVerifier;
import com.paymentplatform.identity.infrastructure.test.TestOrganizationStatusPort;
import com.paymentplatform.shared.domain.exception.ForbiddenException;
import com.paymentplatform.shared.domain.exception.ServiceUnavailableException;
import com.paymentplatform.shared.domain.exception.UnauthorizedException;
import com.paymentplatform.shared.domain.model.OrganizationId;
import com.paymentplatform.shared.domain.model.RoleCode;
import com.paymentplatform.shared.domain.model.UserId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * OAuth passwordless sur H2 : clé RSA générée en-test via Nimbus, aucun réseau.
 * Couvre bon token, mauvais aud, expiré, liaison, email inconnu (401, sans
 * création), reuse refresh.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OAuthLoginUseCaseH2Test {

    @Autowired private OAuthLoginUseCase oauthLogin;
    @Autowired private UserRepository users;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private OidcTokenVerifier verifier;
    @Autowired private OidcProperties oidcProperties;
    @Autowired private RefreshTokenService refreshTokens;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TestOrganizationStatusPort organizationStatus;

    private RSAKey googleKey;
    private RSAKey microsoftKey;

    @BeforeEach
    void setUp() throws Exception {
        googleKey = new RSAKeyGenerator(2048).keyID("google-test-key").generate();
        microsoftKey = new RSAKeyGenerator(2048).keyID("ms-test-key").generate();
        verifier.registerTestKey("google", googleKey.toPublicJWK());
        verifier.registerTestKey("microsoft", microsoftKey.toPublicJWK());
        // Isolation inter-classes : AuthUseCaseH2Test désactive cette org via le port
        // partagé (singleton, non rollbacké) — on la réactive pour être ordre-indépendant.
        organizationStatus.setStatus(UUID.fromString("00000000-0000-0000-0000-000000000005"), "SHOP", "ACTIVE");
    }

    @AfterEach
    void tearDown() {
        verifier.clearTestKeys();
    }

    @Test
    void google_validToken_existingActiveUser_linksAndIssuesPair() throws Exception {
        User local = User.create(new UserId(null), Username.of("oauth.link"),
                Email.of("oauth.link@example.com"),
                PasswordHash.of(passwordEncoder.encode("Secret@1")),
                "Local", "User", new PhoneNumber(null),
                OrganizationId.of(UUID.fromString("00000000-0000-0000-0000-000000000005")),
                RoleCode.SHOP_AGENT);
        users.save(local);

        String idToken = idToken(googleKey, "https://accounts.google.com",
                oidcProperties.googleClientId(), Instant.now().plusSeconds(300),
                "google-sub-link-1", "oauth.link@example.com", true);

        var response = oauthLogin.login(new OAuthLoginRequest("google", idToken));

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.user().email()).isEqualTo("oauth.link@example.com");

        User linked = users.findByEmail(Email.of("oauth.link@example.com")).orElseThrow();
        assertThat(linked.authProvider().name()).isEqualTo("GOOGLE");
        assertThat(linked.providerSubject()).isEqualTo("google-sub-link-1");
        assertThat(linked.emailVerified()).isTrue();
    }

    @Test
    void google_wrongAudience_throws401() throws Exception {
        String idToken = idToken(googleKey, "https://accounts.google.com",
                "wrong-audience", Instant.now().plusSeconds(300),
                "sub-bad-aud", "oauth.badaud@example.com", true);

        assertThatThrownBy(() -> oauthLogin.login(new OAuthLoginRequest("google", idToken)))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Audience");
    }

    @Test
    void google_expiredToken_throws401() throws Exception {
        String idToken = idToken(googleKey, "https://accounts.google.com",
                oidcProperties.googleClientId(), Instant.now().minusSeconds(300),
                "sub-expired", "oauth.expired@example.com", true);

        assertThatThrownBy(() -> oauthLogin.login(new OAuthLoginRequest("google", idToken)))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("expiré");
    }

    @Test
    void google_unverifiedEmail_throws401AndCreatesNothing() throws Exception {
        String idToken = idToken(googleKey, "https://accounts.google.com",
                oidcProperties.googleClientId(), Instant.now().plusSeconds(300),
                "sub-unverified", "oauth.unverified@example.com", false);

        assertThatThrownBy(() -> oauthLogin.login(new OAuthLoginRequest("google", idToken)))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("non vérifié");
        assertThat(users.findByEmail(Email.of("oauth.unverified@example.com"))).isEmpty();
    }

    @Test
    void google_unknownEmail_throws401AndCreatesNothing() throws Exception {
        String idToken = idToken(googleKey, "https://accounts.google.com",
                oidcProperties.googleClientId(), Instant.now().plusSeconds(300),
                "google-sub-new-1", "oauth.new@example.com", true);

        // Pas de création automatique : l'email doit pré-exister dans les utilisateurs.
        assertThatThrownBy(() -> oauthLogin.login(new OAuthLoginRequest("google", idToken)))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Aucun compte associé");

        assertThat(users.findByEmail(Email.of("oauth.new@example.com"))).isEmpty();
    }

    @Test
    void google_disabledExistingUser_throws403() throws Exception {
        User local = User.create(new UserId(null), Username.of("oauth.disabled"),
                Email.of("oauth.disabled@example.com"),
                PasswordHash.of(passwordEncoder.encode("Secret@1")),
                "Dis", "Abled", new PhoneNumber(null), null, RoleCode.SHOP_AGENT);
        local.disable();
        users.save(local);

        String idToken = idToken(googleKey, "https://accounts.google.com",
                oidcProperties.googleClientId(), Instant.now().plusSeconds(300),
                "google-sub-disabled", "oauth.disabled@example.com", true);

        assertThatThrownBy(() -> oauthLogin.login(new OAuthLoginRequest("google", idToken)))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("en attente de validation");
    }

    @Test
    void google_oauthIssuedRefresh_rotatesAndDetectsReuse() throws Exception {
        User local = User.create(new UserId(null), Username.of("oauth.reuse"),
                Email.of("oauth.reuse@example.com"),
                PasswordHash.of(passwordEncoder.encode("Secret@1")),
                "Re", "Use", new PhoneNumber(null),
                OrganizationId.of(UUID.fromString("00000000-0000-0000-0000-000000000005")),
                RoleCode.SHOP_AGENT);
        users.save(local);

        String idToken = idToken(googleKey, "https://accounts.google.com",
                oidcProperties.googleClientId(), Instant.now().plusSeconds(300),
                "google-sub-reuse", "oauth.reuse@example.com", true);
        var login = oauthLogin.login(new OAuthLoginRequest("google", idToken));

        // Rotation : l'ancien refresh OAuth est révoqué, le nouveau fonctionne.
        // Reuse-détection conservée (même comportement que le login password : 401 sur
        // rejeu, sans kill en cascade — le token courant reste valide).
        var rotated = refreshTokens.refresh(login.refreshToken());
        assertThat(rotated.refreshToken()).isNotBlank().isNotEqualTo(login.refreshToken());

        assertThatThrownBy(() -> refreshTokens.refresh(login.refreshToken()))
                .isInstanceOf(UnauthorizedException.class);
        assertThat(refreshTokens.refresh(rotated.refreshToken()).accessToken()).isNotBlank();
    }

    @Test
    void microsoft_validToken_links() throws Exception {
        User local = User.create(new UserId(null), Username.of("oauth.mslink"),
                Email.of("oauth.mslink@example.com"),
                PasswordHash.of(passwordEncoder.encode("Secret@1")),
                "Ms", "Link", new PhoneNumber(null), null, RoleCode.SHOP_AGENT);
        users.save(local);

        String iss = "https://login.microsoftonline.com/" + oidcProperties.microsoftTenantId() + "/v2.0";
        String idToken = idToken(microsoftKey, iss,
                oidcProperties.microsoftClientId(), Instant.now().plusSeconds(300),
                "ms-sub-1", "oauth.mslink@example.com", true);

        var response = oauthLogin.login(new OAuthLoginRequest("microsoft", idToken));
        assertThat(response.accessToken()).isNotBlank();

        User linked = users.findByEmail(Email.of("oauth.mslink@example.com")).orElseThrow();
        assertThat(linked.authProvider().name()).isEqualTo("MICROSOFT");
        assertThat(linked.providerSubject()).isEqualTo("ms-sub-1");
    }

    @Test
    void providerDisabled_throws503() {
        var disabled = new OidcTokenVerifier(new OidcProperties("", "", ""), objectMapper);
        assertThatThrownBy(() -> disabled.verify("google", "dummy"))
                .isInstanceOf(ServiceUnavailableException.class);
        assertThatThrownBy(() -> disabled.verify("microsoft", "dummy"))
                .isInstanceOf(ServiceUnavailableException.class);
    }

    private String idToken(RSAKey key, String iss, String aud, Instant exp,
                           String sub, String email, boolean verified) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(iss)
                .audience(aud)
                .expirationTime(Date.from(exp))
                .issueTime(new Date())
                .subject(sub)
                .claim("email", email)
                .claim("email_verified", verified)
                .build();
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(), claims);
        jwt.sign(new RSASSASigner(key));
        return jwt.serialize();
    }
}
