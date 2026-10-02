package com.paymentplatform.identity.application.usecase;

import com.paymentplatform.identity.domain.model.User;
import com.paymentplatform.identity.domain.repository.UserRepository;
import com.paymentplatform.identity.domain.valueobject.Email;
import com.paymentplatform.identity.domain.valueobject.PasswordHash;
import com.paymentplatform.identity.infrastructure.email.EmailService;
import com.paymentplatform.identity.infrastructure.email.PasswordResetToken;
import com.paymentplatform.identity.infrastructure.email.PasswordResetTokenRepository;
import com.paymentplatform.shared.domain.exception.ConflictException;
import com.paymentplatform.shared.domain.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class PasswordResetUseCase {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetUseCase.class);

    private final UserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final RefreshTokenService refreshTokens;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.password-reset.token-expiry-minutes:30}")
    private int tokenExpiryMinutes;

    public PasswordResetUseCase(UserRepository users, PasswordResetTokenRepository tokens,
                                PasswordEncoder passwordEncoder, EmailService emailService,
                                RefreshTokenService refreshTokens) {
        this.users = users;
        this.tokens = tokens;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.refreshTokens = refreshTokens;
    }

    /**
     * Demande de réinitialisation : 404 explicite si l'email est inconnu
     * (le front affiche le message et reste sur la page).
     */
    @Transactional
    public void requestReset(String email) {
        User user = users.findByEmail(Email.of(email))
                .orElseThrow(() -> new NotFoundException("Aucun compte associé à cet email"));
        log.info("Demande de reset pour {}", email);

        tokens.deleteByUserId(user.id().value());

        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        String token = HexFormat.of().formatHex(tokenBytes);

        Instant expiresAt = Instant.now().plus(tokenExpiryMinutes, ChronoUnit.MINUTES);
        tokens.save(new PasswordResetToken(user.id().value(), token, expiresAt));

        emailService.sendPasswordResetEmail(user.email().value(), user.firstName(), token);
    }

    /** Vérifie qu'un token est valide. */
    public boolean isValidToken(String token) {
        Optional<PasswordResetToken> resetToken = tokens.findByTokenAndUsedFalse(token);
        return resetToken.isPresent() && !resetToken.get().isExpired();
    }

    /** Réinitialise le mot de passe via un token valide (single-use). */
    @Transactional
    public void confirmReset(String token, String newPassword) {
        PasswordResetToken resetToken = tokens.findByTokenAndUsedFalse(token)
                .orElseThrow(() -> new ConflictException("Token invalide ou déjà utilisé"));

        if (resetToken.isExpired()) {
            throw new ConflictException("Token expiré. Veuillez demander un nouveau lien.");
        }

        User user = users.findById(new com.paymentplatform.shared.domain.model.UserId(resetToken.getUserId()))
                .orElseThrow(() -> new NotFoundException("Utilisateur non trouvé"));

        user.changePassword(PasswordHash.of(passwordEncoder.encode(newPassword)));
        users.save(user);

        resetToken.markUsed();
        tokens.save(resetToken);

        // Les sessions existantes sont révoquées : reconnexion obligatoire.
        refreshTokens.revokeAll(user.id().value());
    }
}
