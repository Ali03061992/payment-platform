package com.paymentplatform.identity.infrastructure.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

/**
 * Configuration WebAuthn / Passkeys.
 * En production, rp-id doit être le domaine HTTPS exact (sans schéma ni port)
 * et allowed-origins la liste stricte des origines servies par la PWA.
 */
@ConfigurationProperties(prefix = "app.webauthn")
public record WebAuthnProperties(
        @DefaultValue("localhost") String rpId,
        @DefaultValue("NEXTERA") String rpName,
        @DefaultValue("http://localhost:4200,http://localhost:8080") List<String> allowedOrigins,
        @DefaultValue("300") long challengeTtlSeconds,
        @DefaultValue("60") long timeoutSeconds) {
}
