package com.paymentplatform.identity.application.usecase;

import com.paymentplatform.identity.application.dto.RefreshResponse;
import com.paymentplatform.identity.application.port.TokenIssuer;
import com.paymentplatform.identity.domain.model.RefreshToken;
import com.paymentplatform.identity.domain.model.User;
import com.paymentplatform.identity.domain.repository.RefreshTokenRepository;
import com.paymentplatform.identity.domain.repository.UserRepository;
import com.paymentplatform.shared.domain.exception.UnauthorizedException;
import com.paymentplatform.shared.domain.model.UserId;
import com.paymentplatform.shared.infrastructure.audit.AuditActions;
import com.paymentplatform.shared.infrastructure.audit.AuditRecorder;
import com.paymentplatform.shared.infrastructure.security.AuthenticatedUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * M1 : émission, rotation et révocation des refresh tokens opaques.
 *
 * <p>Le brut (256 bits aléatoires) n'est transmis qu'à l'émission ; seul son
 * hash SHA-256 est persisté. Chaque utilisation fait tourner le token
 * (l'ancien est révoqué avec traçabilité {@code replaced_by}). Un token
 * révoqué/expiré/inconnu — ou un utilisateur désactivé — donne 401.</p>
 */
@Service
public class RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository tokens;
    private final UserRepository users;
    private final TokenIssuer tokenIssuer;
    private final AuditRecorder audit;
    private final long ttlDays;

    public RefreshTokenService(RefreshTokenRepository tokens, UserRepository users,
                               TokenIssuer tokenIssuer, AuditRecorder audit,
                               @Value("${app.auth.refresh-token-ttl-days:7}") long ttlDays) {
        this.tokens = tokens;
        this.users = users;
        this.tokenIssuer = tokenIssuer;
        this.audit = audit;
        this.ttlDays = ttlDays;
    }

    /** Paire brute + entité persistée (le brut ne doit plus jamais être relu en base). */
    public record IssuedRefreshToken(String rawToken, RefreshToken stored, long expiresInSeconds) {
    }

    @Transactional
    public IssuedRefreshToken issue(UUID userId) {
        String raw = generateRawToken();
        RefreshToken stored = tokens.save(RefreshToken.issue(userId, sha256(raw),
                Instant.now().plus(ttlDays, ChronoUnit.DAYS)));
        return new IssuedRefreshToken(raw, stored,
                ChronoUnit.DAYS.getDuration().multipliedBy(ttlDays).getSeconds());
    }

    @Transactional
    public RefreshResponse refresh(String rawToken) {
        RefreshToken current = tokens.findByTokenHash(sha256(rawToken))
                .orElseThrow(() -> new UnauthorizedException("Refresh token invalide"));
        if (!current.isActive()) {
            throw new UnauthorizedException("Refresh token expiré ou révoqué");
        }

        User user = users.findById(UserId.of(current.userId()))
                .orElseThrow(() -> new UnauthorizedException("Utilisateur introuvable"));
        if (!user.isActive()) {
            tokens.revokeAllByUserId(user.id().value());
            throw new UnauthorizedException("Compte désactivé");
        }

        // Rotation : l'ancien est révoqué (traçabilité du successeur), un nouveau est émis.
        IssuedRefreshToken next = issue(user.id().value());
        current.markReplaced(next.stored().tokenHash());
        tokens.save(current);

        List<String> roles = user.roles().stream().map(Enum::name).toList();
        var authenticated = new AuthenticatedUser(user.id().value(), user.username().value(), roles,
                user.organizationId() == null ? null : user.organizationId().value());
        String accessToken = tokenIssuer.issue(authenticated);

        audit.record(user.id().value(),
                user.organizationId() == null ? null : user.organizationId().value(),
                AuditActions.USER_TOKEN_REFRESH, user.id().value(), null);

        return RefreshResponse.of(accessToken, tokenIssuer.expirationSeconds(),
                next.rawToken(), next.expiresInSeconds());
    }

    /** Révocation idempotente (logout) : token inconnu => no-op. */
    @Transactional
    public void revoke(String rawToken) {
        tokens.findByTokenHash(sha256(rawToken)).ifPresent(token -> {
            if (!token.revoked()) {
                token.revoke();
                tokens.save(token);
            }
        });
    }

    @Transactional
    public void revokeAll(UUID userId) {
        tokens.revokeAllByUserId(userId);
    }

    static String generateRawToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    static String sha256(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }
}
