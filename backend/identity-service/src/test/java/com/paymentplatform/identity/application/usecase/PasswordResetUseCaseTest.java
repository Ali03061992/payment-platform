package com.paymentplatform.identity.application.usecase;

import com.paymentplatform.identity.domain.model.User;
import com.paymentplatform.identity.domain.repository.UserRepository;
import com.paymentplatform.identity.domain.valueobject.Email;
import com.paymentplatform.identity.infrastructure.email.EmailService;
import com.paymentplatform.identity.infrastructure.email.PasswordResetToken;
import com.paymentplatform.identity.infrastructure.email.PasswordResetTokenRepository;
import com.paymentplatform.shared.domain.exception.ConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Mot de passe oublié : réponse générique, token single-use avec expiry,
 * révocation des sessions à la confirmation.
 */
@ExtendWith(MockitoExtension.class)
class PasswordResetUseCaseTest {

    @Mock
    private UserRepository users;
    @Mock
    private PasswordResetTokenRepository tokens;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EmailService emailService;
    @Mock
    private RefreshTokenService refreshTokens;
    @Mock
    private User user;

    @InjectMocks
    private PasswordResetUseCase useCase;

    @Test
    void requestReset_unknownEmail_notFoundWithoutSideEffect() {
        when(users.findByEmail(Email.of("inconnu@e2e.test"))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.requestReset("inconnu@e2e.test"))
                .isInstanceOf(com.paymentplatform.shared.domain.exception.NotFoundException.class);

        verify(tokens, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(any(), any(), any());
    }

    @Test
    void requestReset_knownEmail_savesTokenAndSendsEmail() {
        UUID userId = UUID.randomUUID();
        when(users.findByEmail(Email.of("ali@e2e.test"))).thenReturn(Optional.of(user));
        when(user.id()).thenReturn(new com.paymentplatform.shared.domain.model.UserId(userId));
        when(user.email()).thenReturn(Email.of("ali@e2e.test"));
        when(user.firstName()).thenReturn("Ali");

        useCase.requestReset("ali@e2e.test");

        verify(tokens).deleteByUserId(userId);
        verify(tokens).save(any(PasswordResetToken.class));
        verify(emailService).sendPasswordResetEmail(any(), any(), any());
    }

    @Test
    void isValidToken_unknownToken_false() {
        when(tokens.findByTokenAndUsedFalse("nope")).thenReturn(Optional.empty());

        assertThat(useCase.isValidToken("nope")).isFalse();
    }

    @Test
    void confirmReset_expiredToken_conflict() {
        PasswordResetToken expired = new PasswordResetToken(UUID.randomUUID(), "tok",
                Instant.now().minus(1, ChronoUnit.HOURS));
        when(tokens.findByTokenAndUsedFalse("tok")).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> useCase.confirmReset("tok", "NouveauPass123"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void confirmReset_unknownToken_conflict() {
        when(tokens.findByTokenAndUsedFalse("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.confirmReset("nope", "NouveauPass123"))
                .isInstanceOf(ConflictException.class);
    }
}
