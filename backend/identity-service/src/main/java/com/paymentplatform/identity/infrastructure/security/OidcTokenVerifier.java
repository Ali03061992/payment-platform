package com.paymentplatform.identity.infrastructure.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.paymentplatform.shared.domain.exception.ServiceUnavailableException;
import com.paymentplatform.shared.domain.exception.UnauthorizedException;
import org.springframework.stereotype.Service;

import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Vérificateur OIDC générique (Google / Microsoft) basé sur Nimbus JOSE.
 *
 * <p>JWKS distants mis en cache (instances {@link RemoteJWKSet} réutilisées) avec
 * timeouts courts (3s connect/read). Vérifie signature / iss / aud (clientId) / exp,
 * retourne email + emailVerified + subject. Config via env (jamais committée) :
 * GOOGLE_CLIENT_ID, MICROSOFT_CLIENT_ID, MICROSOFT_TENANT_ID — vide = provider
 * désactivé (503 explicite).</p>
 */
@Service
public class OidcTokenVerifier {

    private static final String GOOGLE_JWKS_URL = "https://www.googleapis.com/oauth2/v3/certs";
    private static final Set<String> GOOGLE_ISSUERS =
            Set.of("accounts.google.com", "https://accounts.google.com");
    private static final String MICROSOFT_DISCOVERY_TEMPLATE =
            "https://login.microsoftonline.com/%s/v2.0/.well-known/openid-configuration";
    private static final String MICROSOFT_JWKS_FALLBACK_TEMPLATE =
            "https://login.microsoftonline.com/%s/discovery/v2.0/keys";
    private static final String MICROSOFT_ISS_TEMPLATE =
            "https://login.microsoftonline.com/%s/v2.0";

    private static final int CONNECT_TIMEOUT_MS = 3000;
    private static final int READ_TIMEOUT_MS = 3000;
    private static final long CLOCK_SKEW_SECONDS = 60;

    private final OidcProperties properties;
    private final ObjectMapper objectMapper;

    private final ConcurrentHashMap<String, JWKSource<SecurityContext>> jwkCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, JWK> testKeys = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CachedJwks> microsoftJwksCache = new ConcurrentHashMap<>();

    public OidcTokenVerifier(OidcProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /** Jeton vérifié : email + flag de vérification + subject provider. */
    public record VerifiedOidcToken(String email, boolean emailVerified, String subject) {
    }

    private record CachedJwks(String uri, Instant expiresAt) {
    }

    /**
     * Enregistre une clé de test (aucun réseau). Utilisé par les tests H2/MockMvc
     * avec une clé RSA générée en-test via Nimbus.
     */
    public void registerTestKey(String provider, JWK jwk) {
        if (provider != null && jwk != null) {
            testKeys.put(normalize(provider), jwk);
        }
    }

    public void clearTestKeys() {
        testKeys.clear();
    }

    /**
     * Vérifie un idToken OIDC et en extrait les claims utiles.
     *
     * @param provider google|microsoft (insensible à la casse)
     * @param idToken JWT compact
     * @return email + vérification + subject
     * @throws ServiceUnavailableException provider désactivé ou JWKS injoignable (503)
     * @throws UnauthorizedException signature/iss/aud/exp/email invalides (401)
     */
    public VerifiedOidcToken verify(String provider, String idToken) {
        if (provider == null || provider.isBlank()) {
            throw new UnauthorizedException("Fournisseur OAuth manquant");
        }
        if (idToken == null || idToken.isBlank()) {
            throw new UnauthorizedException("Jeton OAuth manquant");
        }
        String key = normalize(provider);

        String expectedAud;
        Set<String> expectedIssuers;
        String jwksUrl;
        switch (key) {
            case "google" -> {
                if (properties.googleClientId().isBlank()) {
                    throw new ServiceUnavailableException(
                            "Authentification Google désactivée (GOOGLE_CLIENT_ID non configuré)");
                }
                expectedAud = properties.googleClientId();
                expectedIssuers = GOOGLE_ISSUERS;
                jwksUrl = GOOGLE_JWKS_URL;
            }
            case "microsoft" -> {
                if (properties.microsoftClientId().isBlank() || properties.microsoftTenantId().isBlank()) {
                    throw new ServiceUnavailableException(
                            "Authentification Microsoft désactivée "
                                    + "(MICROSOFT_CLIENT_ID / MICROSOFT_TENANT_ID non configurés)");
                }
                expectedAud = properties.microsoftClientId();
                expectedIssuers = Set.of(
                        String.format(MICROSOFT_ISS_TEMPLATE, properties.microsoftTenantId()));
                jwksUrl = resolveMicrosoftJwksUri(properties.microsoftTenantId());
            }
            default -> throw new UnauthorizedException("Fournisseur OAuth inconnu : " + provider);
        }

        SignedJWT jwt;
        try {
            jwt = SignedJWT.parse(idToken);
        } catch (Exception e) {
            throw new UnauthorizedException("Jeton OAuth invalide");
        }

        JWTClaimsSet claims;
        try {
            claims = jwt.getJWTClaimsSet();
        } catch (Exception e) {
            throw new UnauthorizedException("Jeton OAuth illisible");
        }

        Date exp = claims.getExpirationTime();
        if (exp == null || Instant.now().isAfter(exp.toInstant().plusSeconds(CLOCK_SKEW_SECONDS))) {
            throw new UnauthorizedException("Jeton OAuth expiré");
        }

        if (!verifySignature(jwt, key, jwksUrl)) {
            throw new UnauthorizedException("Signature du jeton OAuth invalide");
        }

        String iss = claims.getIssuer();
        if (iss == null || !expectedIssuers.contains(iss)) {
            throw new UnauthorizedException("Émetteur OAuth invalide");
        }

        List<String> aud;
        try {
            aud = claims.getAudience();
        } catch (Exception e) {
            aud = null;
        }
        if (aud == null || !aud.contains(expectedAud)) {
            throw new UnauthorizedException("Audience OAuth invalide");
        }

        String subject = claims.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new UnauthorizedException("Subject OAuth manquant");
        }

        String email;
        try {
            email = claims.getStringClaim("email");
        } catch (Exception e) {
            email = null;
        }
        if (email == null || email.isBlank()) {
            throw new UnauthorizedException("Email OAuth manquant");
        }

        boolean verified = extractEmailVerified(claims);
        return new VerifiedOidcToken(email.trim(), verified, subject);
    }

    private boolean verifySignature(SignedJWT jwt, String providerKey, String jwksUrl) {
        JWK testKey = testKeys.get(providerKey);
        if (testKey != null) {
            return verifyWithKey(jwt, testKey);
        }
        JWKSource<SecurityContext> source = jwkSourceFor(providerKey, jwksUrl);
        String kid;
        try {
            kid = jwt.getHeader().getKeyID();
        } catch (Exception e) {
            kid = null;
        }
        List<JWK> candidates = selectKeys(source, kid);
        if (candidates == null || candidates.isEmpty()) {
            return false;
        }
        for (JWK candidate : candidates) {
            if (verifyWithKey(jwt, candidate)) {
                return true;
            }
        }
        return false;
    }

    private List<JWK> selectKeys(JWKSource<SecurityContext> source, String kid) {
        try {
            JWKSelector selector = kid != null
                    ? new JWKSelector(new JWKMatcher.Builder().keyID(kid).build())
                    : new JWKSelector(new JWKMatcher.Builder().build());
            List<JWK> matches = source.get(selector, null);
            if (matches != null && !matches.isEmpty()) {
                return matches;
            }
            // Rotation : le kid a pu changer — réessaie sans filtre (le cache RemoteJWKSet
            // se rafraîchit sur appel distant si besoin).
            if (kid != null) {
                return source.get(new JWKSelector(new JWKMatcher.Builder().build()), null);
            }
            return matches;
        } catch (ServiceUnavailableException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceUnavailableException("Clés OAuth injoignables (réseau/JWKS)", e);
        }
    }

    private JWKSource<SecurityContext> jwkSourceFor(String providerKey, String jwksUrl) {
        return jwkCache.computeIfAbsent(providerKey + "|" + jwksUrl, k -> {
            try {
                return new RemoteJWKSet<>(new URL(jwksUrl),
                        new DefaultResourceRetriever(CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS));
            } catch (Exception e) {
                throw new IllegalStateException("URL JWKS OAuth invalide : " + jwksUrl, e);
            }
        });
    }

    private String resolveMicrosoftJwksUri(String tenant) {
        CachedJwks cached = microsoftJwksCache.get(tenant);
        if (cached != null && Instant.now().isBefore(cached.expiresAt())) {
            return cached.uri();
        }
        String discoveryUrl = String.format(MICROSOFT_DISCOVERY_TEMPLATE, tenant);
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(discoveryUrl).openConnection();
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            if (conn.getResponseCode() != 200) {
                throw new IllegalStateException("HTTP " + conn.getResponseCode());
            }
            try (var in = conn.getInputStream()) {
                JsonNode node = objectMapper.readTree(in);
                String jwksUri = node.has("jwks_uri") ? node.get("jwks_uri").asText() : null;
                if (jwksUri == null || jwksUri.isBlank()) {
                    throw new IllegalStateException("jwks_uri manquant");
                }
                microsoftJwksCache.put(tenant,
                        new CachedJwks(jwksUri, Instant.now().plus(1, ChronoUnit.HOURS)));
                return jwksUri;
            }
        } catch (Exception e) {
            // Repli : endpoint de clés statique Microsoft (évite un 503 si seul le
            // discovery est indisponible ; l'échec JWKS sera mappé en 503 plus tard).
            return String.format(MICROSOFT_JWKS_FALLBACK_TEMPLATE, tenant);
        }
    }

    private boolean verifyWithKey(SignedJWT jwt, JWK jwk) {
        try {
            JWSVerifier verifier;
            if (jwk instanceof RSAKey rsa) {
                verifier = new RSASSAVerifier(rsa.toRSAPublicKey());
            } else if (jwk instanceof ECKey ec) {
                verifier = new ECDSAVerifier(ec.toECPublicKey());
            } else {
                return false;
            }
            return jwt.verify(verifier);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean extractEmailVerified(JWTClaimsSet claims) {
        try {
            Object raw = claims.getClaim("email_verified");
            if (raw instanceof Boolean b) {
                return b;
            }
            if (raw instanceof String s) {
                return Boolean.parseBoolean(s.trim());
            }
            Boolean b = claims.getBooleanClaim("email_verified");
            return Boolean.TRUE.equals(b);
        } catch (Exception e) {
            return false;
        }
    }

    private String normalize(String provider) {
        return provider.trim().toLowerCase();
    }
}
