package com.paymentplatform.identity.application.usecase;

import java.util.UUID;

import com.paymentplatform.identity.application.dto.LoginRequest;
import com.paymentplatform.identity.domain.model.User;
import com.paymentplatform.identity.domain.model.UserStatus;
import com.paymentplatform.identity.domain.repository.UserRepository;
import com.paymentplatform.identity.domain.valueobject.Email;
import com.paymentplatform.identity.domain.valueobject.PasswordHash;
import com.paymentplatform.identity.domain.valueobject.PhoneNumber;
import com.paymentplatform.identity.domain.valueobject.Username;
import com.paymentplatform.identity.infrastructure.test.TestOrganizationStatusPort;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthUseCaseH2Test {

    @Autowired
    private AuthUseCase auth;

    @Autowired
    private UserRepository users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TestOrganizationStatusPort organizationStatus;

    @BeforeEach
    void setUp() {
        User agent = User.create(new UserId(null), Username.of("shop.agent"),
                Email.of("shop.agent@example.com"), PasswordHash.of(passwordEncoder.encode("Secret@1")),
                "Amine", "Trabelsi", new PhoneNumber(null),
                OrganizationId.of(UUID.fromString("00000000-0000-0000-0000-000000000005")), RoleCode.SHOP_AGENT);
        users.save(agent);
    }

    @Test
    void login_success_issuesToken() {
        var response = auth.login(new LoginRequest("shop.agent", "Secret@1"));

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.expiresIn()).isGreaterThan(0);
        assertThat(response.user().roles()).contains("SHOP_AGENT");
    }

    @Test
    void login_wrongPassword_throws() {
        assertThatThrownBy(() -> auth.login(new LoginRequest("shop.agent", "wrong")))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void login_disabledUser_throws() {
        User agent = users.findByUsername(Username.of("shop.agent")).orElseThrow();
        agent.disable();
        users.save(agent);

        assertThatThrownBy(() -> auth.login(new LoginRequest("shop.agent", "Secret@1")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("désactivé");
    }

    @Test
    void login_disabledOrganization_throws() {
        organizationStatus.setStatus(UUID.fromString("00000000-0000-0000-0000-000000000005"), "SHOP", "DISABLED");

        assertThatThrownBy(() -> auth.login(new LoginRequest("shop.agent", "Secret@1")))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void login_unknownUser_throws() {
        assertThatThrownBy(() -> auth.login(new LoginRequest("ghost", "x")))
                .isInstanceOf(UnauthorizedException.class);
    }
}
