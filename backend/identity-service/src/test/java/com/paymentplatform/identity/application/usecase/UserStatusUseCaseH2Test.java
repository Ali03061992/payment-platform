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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserStatusUseCaseH2Test {

    @Autowired private UserStatusUseCase status;
    @Autowired private UserRepository users;
    @Autowired private PasswordEncoder passwordEncoder;

    private long userId;

    @BeforeEach
    void setUp() {
        User user = User.create(new UserId(0), Username.of("ustatus.user"),
                Email.of("ustatus@test.com"), PasswordHash.of(passwordEncoder.encode("pass")),
                "Status", "Test", new PhoneNumber(null),
                OrganizationId.of(5), RoleCode.SHOP_AGENT);
        users.save(user);
        User fetched = users.findByUsername(Username.of("ustatus.user")).orElseThrow();
        userId = fetched.id().value();
    }

    @Test
    void disableUser_activeUser_becomesDisabled() {
        var response = status.disableUser(1L, userId);
        assertThat(response.status()).isEqualTo("DISABLED");
    }

    @Test
    void disableUser_alreadyDisabled_idempotent() {
        status.disableUser(1L, userId);
        var response = status.disableUser(1L, userId);
        assertThat(response.status()).isEqualTo("DISABLED");
    }

    @Test
    void activateUser_disabledUser_becomesActive() {
        status.disableUser(1L, userId);
        var response = status.activateUser(1L, userId);
        assertThat(response.status()).isEqualTo("ACTIVE");
    }

    @Test
    void activateUser_alreadyActive_idempotent() {
        var response = status.activateUser(1L, userId);
        assertThat(response.status()).isEqualTo("ACTIVE");
    }

    @Test
    void disableUser_notFound_throws() {
        assertThatThrownBy(() -> status.disableUser(1L, 99999L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void activateUser_notFound_throws() {
        assertThatThrownBy(() -> status.activateUser(1L, 99999L))
                .isInstanceOf(NotFoundException.class);
    }
}
