package com.paymentplatform.identity.infrastructure.configuration;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Configuration WebAuthn / Passkeys.
 * En production, rp-id doit être le domaine HTTPS exact (sans schéma ni port)
 * et allowed-origins la liste stricte des origines servies par la PWA.
 */
@Validated
@ConfigurationProperties(prefix = "app.webauthn")
public record WebAuthnProperties(
        @NotBlank @DefaultValue("localhost") String rpId,
        @NotBlank @DefaultValue("NEXTERA") String rpName,
        @NotEmpty @DefaultValue("http://localhost:4200,http://localhost:8080") List<@NotBlank String> allowedOrigins,
        @Min(30) @DefaultValue("300") long challengeTtlSeconds,
        @Min(10) @DefaultValue("60") long timeoutSeconds) {
}
