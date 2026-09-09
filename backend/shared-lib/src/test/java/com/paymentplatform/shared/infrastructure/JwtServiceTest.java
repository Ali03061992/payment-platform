package com.paymentplatform.shared.infrastructure;

import java.util.UUID;

import com.paymentplatform.shared.infrastructure.security.AuthenticatedUser;
import com.paymentplatform.shared.infrastructure.security.JwtService;
import com.paymentplatform.shared.infrastructure.security.SecurityProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(new SecurityProperties(
                "test-secret-key-for-jwt-signing-minimum-32-bytes-long-ok", Duration.ofMinutes(30)));
    }

    @Test
    void issue_andParse_roundTrip() {
        var user = new AuthenticatedUser(UUID.fromString("00000000-0000-0000-0000-000000000001"), "admin", List.of("SYSTEM_ADMIN"), null);
        String token = jwtService.issue(user);

        assertThat(token).isNotBlank();

        var parsed = jwtService.parse(token);
        assertThat(parsed.userId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        assertThat(parsed.username()).isEqualTo("admin");
        assertThat(parsed.roles()).containsExactly("SYSTEM_ADMIN");
        assertThat(parsed.organizationId()).isNull();
    }

    @Test
    void issue_withOrganizationId() {
        var user = new AuthenticatedUser(UUID.fromString("00000000-0000-0000-0000-000000000010"), "agent", List.of("SUPPLIER_AGENT"), UUID.fromString("00000000-0000-0000-0000-000000000042"));
        String token = jwtService.issue(user);

        var parsed = jwtService.parse(token);
        assertThat(parsed.organizationId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000042"));
    }

    @Test
    void parse_invalidToken_throws() {
        assertThatThrownBy(() -> jwtService.parse("invalid.token.here"))
                .isInstanceOf(Exception.class);
    }

    @Test
    void expirationSeconds_matchesConfig() {
        assertThat(jwtService.expirationSeconds()).isEqualTo(30 * 60);
    }

    @Test
    void issue_insecureSecret_throws() {
        assertThatThrownBy(() -> new JwtService(new SecurityProperties(
                "dev-only-secret-change-me", Duration.ofMinutes(30))))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void issue_shortSecret_throws() {
        assertThatThrownBy(() -> new JwtService(new SecurityProperties(
                "short", Duration.ofMinutes(30))))
                .isInstanceOf(IllegalStateException.class);
    }
}
