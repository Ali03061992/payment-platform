package com.paymentplatform.shared.infrastructure.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.paymentplatform.shared.domain.exception.UnauthorizedException;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Émission et validation des JWT (HS256).
 * Claims : sub=userId, username, roles[], organizationId, iat, exp, jti.
 */
public class JwtService {

    private static final String ISSUER = "payment-platform";
    private static final MacAlgorithm ALGORITHM = MacAlgorithm.HS256;

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final long expirationMinutes;

    public JwtService(SecurityProperties properties) {
        SecretKeySpec key = new SecretKeySpec(
                properties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        this.decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(ALGORITHM).build();
        this.expirationMinutes = properties.expiration() == null
                ? 30
                : properties.expiration().toMinutes();
    }

    public String issue(AuthenticatedUser user) {
        Instant now = Instant.now();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .issuedAt(now)
                .expiresAt(now.plus(expirationMinutes, ChronoUnit.MINUTES))
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(user.userId()))
                .claim("username", user.username())
                .claim("roles", user.roles());
        if (user.organizationId() != null) {
            claims.claim("organizationId", user.organizationId());
        }
        return encoder.encode(JwtEncoderParameters.from(claims.build())).getTokenValue();
    }

    public long expirationSeconds() {
        return expirationMinutes * 60;
    }

    public AuthenticatedUser parse(String token) {
        try {
            Jwt jwt = decoder.decode(token);
            long userId = Long.parseLong(jwt.getSubject());
            String username = jwt.getClaimAsString("username");
            List<String> roles = jwt.getClaimAsStringList("roles");
            Long organizationId = jwt.hasClaim("organizationId")
                    ? jwt.getClaim("organizationId") instanceof Number number ? number.longValue() : null
                    : null;
            return new AuthenticatedUser(userId, username, roles == null ? List.of() : roles, organizationId);
        } catch (Exception e) {
            throw new UnauthorizedException("JWT invalide ou expiré");
        }
    }
}