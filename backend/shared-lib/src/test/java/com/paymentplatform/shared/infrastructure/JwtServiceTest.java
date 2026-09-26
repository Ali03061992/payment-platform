package com.paymentplatform.shared.infrastructure;

import com.paymentplatform.shared.infrastructure.security.AuthenticatedUser;
import com.paymentplatform.shared.infrastructure.security.JwtService;
import com.paymentplatform.shared.infrastructure.security.SecurityProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
/**
 * Tests de JwtServiceTest.
 * Perimetre : comportement de JwtService (securite/domaine).
 * Moyens : JUnit pur (AssertJ).
 */

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        var env = new MockEnvironment();
        env.setActiveProfiles("local");
        jwtService = new JwtService(new SecurityProperties(
                "test-secret-key-for-jwt-signing-minimum-32-bytes-long-ok", Duration.ofMinutes(30)), env);
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
    void issue_insecureSecret_inProd_throws() {
        var prodEnv = new MockEnvironment();
        prodEnv.setActiveProfiles("prod");
        assertThatThrownBy(() -> new JwtService(new SecurityProperties(
                "dev-only-secret-change-me", Duration.ofMinutes(30)), prodEnv))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void issue_insecureSecret_inLocal_accepted() {
        var localEnv = new MockEnvironment();
        localEnv.setActiveProfiles("local");
        var service = new JwtService(new SecurityProperties(
                "dev-only-secret-change-me-0123456789abcdef0123456789abcdef", Duration.ofMinutes(30)), localEnv);
        var user = new AuthenticatedUser(UUID.randomUUID(), "test", List.of("SYSTEM_ADMIN"), null);
        String token = service.issue(user);
        assertThat(token).isNotBlank();
    }

    @Test
    void issue_shortSecret_throws() {
        var env = new MockEnvironment();
        env.setActiveProfiles("local");
        assertThatThrownBy(() -> new JwtService(new SecurityProperties(
                "short", Duration.ofMinutes(30)), env))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void gatewayManualValidation_compatibleWithJwtService() throws Exception {
        String secret = "test-secret-key-for-jwt-signing-minimum-32-bytes-long-ok";
        var user = new AuthenticatedUser(UUID.randomUUID(), "test", List.of("SYSTEM_ADMIN"), null);
        String token = jwtService.issue(user);

        String[] parts = token.split("\\.");
        assertThat(parts.length).isEqualTo(3);

        String header = parts[0];
        String payload = parts[1];
        String signature = parts[2];

        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(keyBytes, "HmacSHA256"));

        String expectedSignature = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(mac.doFinal((header + "." + payload).getBytes(StandardCharsets.UTF_8)));

        assertThat(signature).isEqualTo(expectedSignature);
    }
}
