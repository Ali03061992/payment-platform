package com.paymentplatform.shared.infrastructure.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.paymentplatform.shared.domain.exception.UnauthorizedException;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import org.springframework.core.env.Environment;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Émission et validation des JWT (HS256).
 * Claims : sub=userId, username, roles[], organizationId, iat, exp, jti.
 */
public class JwtService {

    private static final String ISSUER = "payment-platform";
    private static final MacAlgorithm ALGORITHM = MacAlgorithm.HS256;

    private final JWSSigner signer;
    private final JwtDecoder decoder;
    private final long expirationMinutes;

    private static final String INSECURE_DEFAULT_SECRET = "dev-only-secret-change-me";

    public JwtService(SecurityProperties properties, Environment environment) {
        String secret = properties.secret();
        boolean isProd = java.util.List.of(environment.getActiveProfiles()).contains("prod");
        if (isProd) {
            if (secret == null || secret.startsWith(INSECURE_DEFAULT_SECRET)) {
                throw new IllegalStateException(
                        "JWT_SECRET must be configured with a secure value in production. " +
                        "Set the JWT_SECRET environment variable.");
            }
        }
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET must be set.");
        }
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET must be at least 32 bytes (256 bits) for HS256 security.");
        }
        SecretKeySpec key = new SecretKeySpec(secretBytes, "HmacSHA256");

        try {
            this.signer = new MACSigner(key);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create JWT signer", e);
        }
        this.decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(ALGORITHM).build();
        this.expirationMinutes = properties.expiration() == null
                ? 30
                : properties.expiration().toMinutes();
    }

    public String issue(AuthenticatedUser user) {
        try {
            Instant now = Instant.now();
            Instant expiry = now.plus(expirationMinutes, ChronoUnit.MINUTES);

            JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                    .issuer(ISSUER)
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(expiry))
                    .jwtID(UUID.randomUUID().toString())
                    .subject(user.userId().toString())
                    .claim("username", user.username())
                    .claim("roles", user.roles());
            if (user.organizationId() != null) {
                claimsBuilder.claim("organizationId", user.organizationId().toString());
            }

            JWSHeader header = new JWSHeader(JWSAlgorithm.HS256);
            SignedJWT signedJWT = new SignedJWT(header, claimsBuilder.build());
            signedJWT.sign(signer);
            return signedJWT.serialize();
        } catch (Exception e) {
            throw new UnauthorizedException("Erreur lors de la génération du JWT");
        }
    }

    public long expirationSeconds() {
        return expirationMinutes * 60;
    }

    public AuthenticatedUser parse(String token) {
        try {
            Jwt jwt = decoder.decode(token);
            UUID userId = UUID.fromString(jwt.getSubject());
            String username = jwt.getClaimAsString("username");
            List<String> roles = jwt.getClaimAsStringList("roles");
            UUID organizationId = jwt.hasClaim("organizationId")
                    ? jwt.hasClaim("organizationId") ? UUID.fromString(jwt.getClaimAsString("organizationId")) : null
                    : null;
            return new AuthenticatedUser(userId, username, roles == null ? List.of() : roles, organizationId);
        } catch (Exception e) {
            throw new UnauthorizedException("JWT invalide ou expiré");
        }
    }
}