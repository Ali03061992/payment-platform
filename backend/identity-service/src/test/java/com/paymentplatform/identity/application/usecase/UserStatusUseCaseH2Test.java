package com.paymentplatform.identity.application.usecase;

import com.paymentplatform.identity.domain.model.User;
import com.paymentplatform.identity.domain.repository.UserRepository;
import com.paymentplatform.identity.domain.valueobject.Email;
import com.paymentplatform.identity.domain.valueobject.PasswordHash;
import com.paymentplatform.identity.domain.valueobject.PhoneNumber;
import com.paymentplatform.identity.domain.valueobject.Username;
import com.paymentplatform.shared.domain.exception.NotFoundException;
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
 * Tests de UserStatusUseCaseH2Test.
 * Perimetre : cas d'usage/service UserStatusUseCase sur base H2.
 * Moyens : contexte SpringBootTest, profil "test" (H2).
 */

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserStatusUseCaseH2Test {

    @Autowired private UserStatusUseCase status;
    @Autowired private UserRepository users;
    @Autowired private PasswordEncoder passwordEncoder;

    private UUID userId;

    @BeforeEach
    void setUp() {
        User user = User.create(new UserId(null), Username.of("ustatus.user"),
                Email.of("ustatus@test.com"), PasswordHash.of(passwordEncoder.encode("pass")),
                "Status", "Test", new PhoneNumber(null),
                OrganizationId.of(UUID.fromString("00000000-0000-0000-0000-000000000005")), RoleCode.SHOP_AGENT);
        users.save(user);
        User fetched = users.findByUsername(Username.of("ustatus.user")).orElseThrow();
        userId = fetched.id().value();
    }

    @Test
    void disableUser_activeUser_becomesDisabled() {
        var response = status.disableUser(UUID.fromString("00000000-0000-0000-0000-000000000001"), userId);
        assertThat(response.status()).isEqualTo("DISABLED");
    }

    @Test
    void disableUser_alreadyDisabled_idempotent() {
        status.disableUser(UUID.fromString("00000000-0000-0000-0000-000000000001"), userId);
        var response = status.disableUser(UUID.fromString("00000000-0000-0000-0000-000000000001"), userId);
        assertThat(response.status()).isEqualTo("DISABLED");
    }

    @Test
    void activateUser_disabledUser_becomesActive() {
        status.disableUser(UUID.fromString("00000000-0000-0000-0000-000000000001"), userId);
        var response = status.activateUser(UUID.fromString("00000000-0000-0000-0000-000000000001"), userId);
        assertThat(response.status()).isEqualTo("ACTIVE");
    }

    @Test
    void activateUser_alreadyActive_idempotent() {
        var response = status.activateUser(UUID.fromString("00000000-0000-0000-0000-000000000001"), userId);
        assertThat(response.status()).isEqualTo("ACTIVE");
    }

    @Test
    void disableUser_notFound_throws() {
        assertThatThrownBy(() -> status.disableUser(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000099999")))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void activateUser_notFound_throws() {
        assertThatThrownBy(() -> status.activateUser(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000099999")))
                .isInstanceOf(NotFoundException.class);
    }
}
