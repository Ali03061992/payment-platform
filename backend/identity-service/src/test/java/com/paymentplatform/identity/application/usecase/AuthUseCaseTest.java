package com.paymentplatform.identity.application.usecase;

import com.paymentplatform.shared.domain.exception.UnauthorizedException;
import com.paymentplatform.shared.infrastructure.audit.AuditRecorder;
import com.paymentplatform.shared.infrastructure.security.AuthenticatedUser;
import com.paymentplatform.identity.application.dto.LoginRequest;
import com.paymentplatform.identity.application.port.OrganizationStatus;
import com.paymentplatform.identity.application.port.OrganizationStatusPort;
import com.paymentplatform.identity.application.port.TokenIssuer;
import com.paymentplatform.identity.domain.model.User;
import com.paymentplatform.identity.domain.model.UserStatus;
import com.paymentplatform.identity.domain.repository.UserRepository;
import com.paymentplatform.identity.domain.valueobject.Email;
import com.paymentplatform.identity.domain.valueobject.PasswordHash;
import com.paymentplatform.identity.domain.valueobject.PhoneNumber;
import com.paymentplatform.identity.domain.valueobject.Username;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthUseCaseTest {

    @Mock
    UserRepository users;
    @Mock
    OrganizationStatusPort organizationStatus;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    TokenIssuer tokenIssuer;
    @Mock
    AuditRecorder audit;

    AuthUseCase auth;

    @BeforeEach
    void setUp() {
        auth = new AuthUseCase(users, organizationStatus, passwordEncoder, tokenIssuer, audit);
    }

    private User activeAgent() {
        return User.create(new com.paymentplatform.shared.domain.model.UserId(11),
                Username.of("shop.agent"), Email.of("shop.agent@example.com"), PasswordHash.of("enc"),
                "Amine", "Trabelsi", new PhoneNumber(null),
                com.paymentplatform.shared.domain.model.OrganizationId.of(5),
                com.paymentplatform.shared.domain.model.RoleCode.SHOP_AGENT);
    }

    @Test
    void login_success_issuesToken() {
        when(users.findByUsername(Username.of("shop.agent"))).thenReturn(Optional.of(activeAgent()));
        when(passwordEncoder.matches("Secret@1", "enc")).thenReturn(true);
        when(organizationStatus.getOrganizationStatus(5))
                .thenReturn(new OrganizationStatus(5, "SHOP", "ACTIVE"));
        when(tokenIssuer.issue(any(AuthenticatedUser.class))).thenReturn("jwt-token");
        when(tokenIssuer.expirationSeconds()).thenReturn(1800L);

        var response = auth.login(new LoginRequest("shop.agent", "Secret@1"));

        assertThat(response.accessToken()).isEqualTo("jwt-token");
        assertThat(response.expiresIn()).isEqualTo(1800);
        assertThat(response.user().roles()).contains("SHOP_AGENT");
    }

    @Test
    void login_wrongPassword_throws() {
        when(users.findByUsername(any())).thenReturn(Optional.of(activeAgent()));
        when(passwordEncoder.matches("wrong", "enc")).thenReturn(false);

        assertThatThrownBy(() -> auth.login(new LoginRequest("shop.agent", "wrong")))
                .isInstanceOf(UnauthorizedException.class);
        verify(tokenIssuer, never()).issue(any());
    }

    @Test
    void login_disabledUser_throws() {
        User user = activeAgent();
        user.disable();
        when(users.findByUsername(any())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(any(), any())).thenReturn(true);

        assertThatThrownBy(() -> auth.login(new LoginRequest("shop.agent", "Secret@1")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("désactivé");
    }

    @Test
    void login_disabledOrganization_throws() {
        when(users.findByUsername(any())).thenReturn(Optional.of(activeAgent()));
        when(passwordEncoder.matches(any(), any())).thenReturn(true);
        when(organizationStatus.getOrganizationStatus(5))
                .thenReturn(new OrganizationStatus(5, "SHOP", "DISABLED"));

        assertThatThrownBy(() -> auth.login(new LoginRequest("shop.agent", "Secret@1")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Organisation désactivée");
    }

    @Test
    void login_unknownUser_throws() {
        when(users.findByUsername(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> auth.login(new LoginRequest("ghost", "x")))
                .isInstanceOf(UnauthorizedException.class);
    }
}