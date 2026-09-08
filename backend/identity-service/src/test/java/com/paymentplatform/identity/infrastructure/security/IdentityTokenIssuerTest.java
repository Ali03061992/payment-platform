package com.paymentplatform.identity.infrastructure.security;

import com.paymentplatform.shared.infrastructure.security.AuthenticatedUser;
import com.paymentplatform.shared.infrastructure.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class IdentityTokenIssuerTest {

    @Autowired
    private IdentityTokenIssuer issuer;

    @Autowired
    private JwtService jwtService;

    @Test
    void issue_returnsToken() {
        AuthenticatedUser user = new AuthenticatedUser(1L, "testuser", List.of("SHOP_AGENT"), 5L);
        String token = issuer.issue(user);
        assertThat(token).isNotBlank();
    }

    @Test
    void issue_tokenCanBeParsed() {
        AuthenticatedUser user = new AuthenticatedUser(1L, "testuser", List.of("SHOP_AGENT"), 5L);
        String token = issuer.issue(user);
        AuthenticatedUser parsed = jwtService.parse(token);
        assertThat(parsed.userId()).isEqualTo(1L);
        assertThat(parsed.username()).isEqualTo("testuser");
        assertThat(parsed.roles()).contains("SHOP_AGENT");
        assertThat(parsed.organizationId()).isEqualTo(5L);
    }

    @Test
    void expirationSeconds_returnsPositive() {
        assertThat(issuer.expirationSeconds()).isGreaterThan(0);
    }

    @Test
    void issue_withNullOrganizationId() {
        AuthenticatedUser user = new AuthenticatedUser(2L, "admin", List.of("SYSTEM_ADMIN"), null);
        String token = issuer.issue(user);
        AuthenticatedUser parsed = jwtService.parse(token);
        assertThat(parsed.organizationId()).isNull();
    }
}
