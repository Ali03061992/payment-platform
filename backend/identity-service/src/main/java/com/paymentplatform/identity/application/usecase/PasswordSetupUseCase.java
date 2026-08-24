package com.paymentplatform.identity.application.usecase;

import com.paymentplatform.identity.infrastructure.email.EmailService;
import com.paymentplatform.identity.infrastructure.email.PasswordSetupToken;
import com.paymentplatform.identity.infrastructure.email.PasswordSetupTokenRepository;
import com.paymentplatform.shared.domain.exception.ConflictException;
import com.paymentplatform.shared.domain.exception.NotFoundException;
import com.paymentplatform.identity.domain.model.User;
import com.paymentplatform.identity.domain.repository.UserRepository;
import com.paymentplatform.identity.domain.valueobject.PasswordHash;
import com.paymentplatform.identity.domain.valueobject.Username;
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
public class PasswordSetupUseCase {

    private final UserRepository users;
    private final PasswordSetupTokenRepository tokens;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.password-setup.token-expiry-hours:24}")
    private int tokenExpiryHours;

    public PasswordSetupUseCase(UserRepository users, PasswordSetupTokenRepository tokens,
                                PasswordEncoder passwordEncoder, EmailService emailService) {
        this.users = users;
        this.tokens = tokens;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    /** Génère un token et envoie l'email de configuration du mot de passe. */
    @Transactional
    public void initiateSetup(String username) {
        User user = users.findByUsername(Username.of(username))
                .orElseThrow(() -> new NotFoundException("Utilisateur non trouvé"));

        tokens.deleteByUserId(user.id().value());

        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        String token = HexFormat.of().formatHex(tokenBytes);

        Instant expiresAt = Instant.now().plus(tokenExpiryHours, ChronoUnit.HOURS);
        PasswordSetupToken setupToken = new PasswordSetupToken(user.id().value(), token, expiresAt);
        tokens.save(setupToken);

        emailService.sendPasswordSetupEmail(user.email().value(), user.firstName(), token);
    }

    /** Vérifie qu'un token est valide. */
    public boolean isValidToken(String token) {
        Optional<PasswordSetupToken> setupToken = tokens.findByTokenAndUsedFalse(token);
        return setupToken.isPresent() && !setupToken.get().isExpired();
    }

    /** Définit le mot de passe via un token valide. */
    @Transactional
    public void completeSetup(String token, String newPassword) {
        PasswordSetupToken setupToken = tokens.findByTokenAndUsedFalse(token)
                .orElseThrow(() -> new ConflictException("Token invalide ou déjà utilisé"));

        if (setupToken.isExpired()) {
            throw new ConflictException("Token expiré. Veuillez demander un nouveau lien.");
        }

        User user = users.findById(new com.paymentplatform.shared.domain.model.UserId(setupToken.getUserId()))
                .orElseThrow(() -> new NotFoundException("Utilisateur non trouvé"));

        user.changePassword(PasswordHash.of(passwordEncoder.encode(newPassword)));
        users.save(user);

        setupToken.markUsed();
        tokens.save(setupToken);
    }
}
