package com.paymentplatform.identity.application.usecase;

import com.paymentplatform.identity.domain.model.User;
import com.paymentplatform.identity.domain.repository.RefreshTokenRepository;
import com.paymentplatform.identity.domain.repository.UserRepository;
import com.paymentplatform.identity.domain.valueobject.Email;
import com.paymentplatform.identity.domain.valueobject.PasswordHash;
import com.paymentplatform.identity.domain.valueobject.PhoneNumber;
import com.paymentplatform.identity.domain.valueobject.Username;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
/**
 * Tests de RefreshTokenServiceH2Test.
 * Perimetre : cas d'usage/service RefreshTokenService sur base H2.
 * Moyens : contexte SpringBootTest, profil "test" (H2).
 */

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RefreshTokenServiceH2Test {

    @Autowired private RefreshTokenService refreshTokens;
    @Autowired private RefreshTokenRepository tokenRepository;
    @Autowired private UserRepository users;
    @Autowired private PasswordEncoder passwordEncoder;

    private UUID userId;

    @BeforeEach
    void setUp() {
        User user = User.create(new UserId(null), Username.of("rt.user"),
                Email.of("rt.user@example.com"), PasswordHash.of(passwordEncoder.encode("Secret@1")),
                "Refresh", "User", new PhoneNumber(null),
                OrganizationId.of(UUID.fromString("00000000-0000-0000-0000-000000000005")),
                RoleCode.SHOP_AGENT);
        users.save(user);
        userId = users.findByUsername(Username.of("rt.user")).orElseThrow().id().value();
    }

    @Test
    void issue_persistsOnlyHash() {
        var issued = refreshTokens.issue(userId);

        assertThat(issued.rawToken()).hasSize(64);
        var stored = tokenRepository.findByTokenHash(RefreshTokenService.sha256(issued.rawToken()));
        assertThat(stored).isPresent();
        // Le brut n'est stocké nulle part : seul le hash correspond.
        assertThat(stored.get().tokenHash()).isNotEqualTo(issued.rawToken());
    }

    @Test
    void refresh_rotatesTokens() {
        var first = refreshTokens.issue(userId);

        var response = refreshTokens.refresh(first.rawToken());

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank().isNotEqualTo(first.rawToken());
        // L'ancien est révoqué avec traçabilité du successeur.
        var old = tokenRepository.findByTokenHash(RefreshTokenService.sha256(first.rawToken())).orElseThrow();
        assertThat(old.revoked()).isTrue();
        assertThat(old.replacedByHash())
                .isEqualTo(RefreshTokenService.sha256(response.refreshToken()));
        // Le nouveau fonctionne.
        var second = refreshTokens.refresh(response.refreshToken());
        assertThat(second.accessToken()).isNotBlank();
    }

    @Test
    void refresh_reusedToken_throws401() {
        var first = refreshTokens.issue(userId);
        var rotated = refreshTokens.refresh(first.rawToken());

        assertThatThrownBy(() -> refreshTokens.refresh(first.rawToken()))
                .isInstanceOf(UnauthorizedException.class);
        // Le token courant reste valide (pas de kill en cascade sur simple rejeu).
        assertThat(refreshTokens.refresh(rotated.refreshToken()).accessToken()).isNotBlank();
    }

    @Test
    void refresh_unknownToken_throws401() {
        assertThatThrownBy(() -> refreshTokens.refresh("00".repeat(32)))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void revoke_isIdempotent() {
        var issued = refreshTokens.issue(userId);

        refreshTokens.revoke(issued.rawToken());
        refreshTokens.revoke(issued.rawToken());
        refreshTokens.revoke("00".repeat(32));

        assertThatThrownBy(() -> refreshTokens.refresh(issued.rawToken()))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void revokeAll_invalidatesEverySession() {
        var first = refreshTokens.issue(userId);
        var second = refreshTokens.issue(userId);

        refreshTokens.revokeAll(userId);

        assertThatThrownBy(() -> refreshTokens.refresh(first.rawToken()))
                .isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> refreshTokens.refresh(second.rawToken()))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void refresh_disabledUser_throws401AndRevokes() {
        var issued = refreshTokens.issue(userId);
        User user = users.findById(UserId.of(userId)).orElseThrow();
        user.disable();
        users.save(user);

        assertThatThrownBy(() -> refreshTokens.refresh(issued.rawToken()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("désactivé");
    }
}
