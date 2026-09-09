package com.paymentplatform.identity.application.usecase;

import java.util.UUID;

import com.paymentplatform.identity.domain.model.User;
import com.paymentplatform.identity.domain.repository.UserRepository;
import com.paymentplatform.identity.domain.valueobject.Email;
import com.paymentplatform.identity.domain.valueobject.PasswordHash;
import com.paymentplatform.identity.domain.valueobject.PhoneNumber;
import com.paymentplatform.identity.domain.valueobject.Username;
import com.paymentplatform.identity.infrastructure.email.PasswordSetupTokenRepository;
import com.paymentplatform.shared.domain.exception.ConflictException;
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
class PasswordSetupUseCaseH2Test {

    @Autowired private PasswordSetupUseCase passwordSetup;
    @Autowired private UserRepository users;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private PasswordSetupTokenRepository tokens;

    @BeforeEach
    void setUp() {
        User user = User.create(new UserId(null), Username.of("pwd.testuser"),
                Email.of("pwd.test@example.com"), PasswordHash.of(passwordEncoder.encode("OldPass@1")),
                "Test", "User", new PhoneNumber(null),
                OrganizationId.of(UUID.fromString("00000000-0000-0000-0000-000000000005")), RoleCode.SHOP_AGENT);
        users.save(user);
    }

    @Test
    void initiateSetup_validUser_sendsEmail() {
        passwordSetup.initiateSetup("pwd.testuser");
        assertThat(tokens.findAll()).isNotEmpty();
    }

    @Test
    void initiateSetup_unknownUser_throwsNotFound() {
        assertThatThrownBy(() -> passwordSetup.initiateSetup("ghost"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void completeSetup_validToken_changesPassword() {
        passwordSetup.initiateSetup("pwd.testuser");
        String token = tokens.findAll().get(0).getToken();
        passwordSetup.completeSetup(token, "NewPass@123");
        assertThat(passwordSetup.isValidToken(token)).isFalse();
    }

    @Test
    void completeSetup_invalidToken_throwsConflict() {
        assertThatThrownBy(() -> passwordSetup.completeSetup("invalid-token", "NewPass@123"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Token invalide");
    }

    @Test
    void isValidToken_unknownToken_returnsFalse() {
        assertThat(passwordSetup.isValidToken("nonexistent")).isFalse();
    }
}
