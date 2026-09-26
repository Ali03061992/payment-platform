package com.paymentplatform.identity.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Configuration OIDC lue depuis l'environnement (jamais committée).
 * Vide = provider désactivé (503 explicite côté vérificateur).
 */
@Component
public class OidcProperties {

    private final String googleClientId;
    private final String microsoftClientId;
    private final String microsoftTenantId;

    public OidcProperties(
            @Value("${app.oauth.google-client-id:}") String googleClientId,
            @Value("${app.oauth.microsoft-client-id:}") String microsoftClientId,
            @Value("${app.oauth.microsoft-tenant-id:}") String microsoftTenantId) {
        this.googleClientId = googleClientId == null ? "" : googleClientId.trim();
        this.microsoftClientId = microsoftClientId == null ? "" : microsoftClientId.trim();
        this.microsoftTenantId = microsoftTenantId == null ? "" : microsoftTenantId.trim();
    }

    public String googleClientId() {
        return googleClientId;
    }

    public String microsoftClientId() {
        return microsoftClientId;
    }

    public String microsoftTenantId() {
        return microsoftTenantId;
    }
}
