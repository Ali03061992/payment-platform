package com.paymentplatform.shared.domain.security;

import org.springframework.core.env.Environment;

import java.util.Set;

/**
 * Garde-fou B3 : le secret inter-services ne doit jamais rester sur une valeur
 * par défaut connue une fois en production, et ne doit jamais être absent.
 *
 * <p>Les {@code @Value("${app.internal-secret}")} sans défaut lèvent déjà une
 * exception au boot si la propriété est absente ; ce validateur ajoute le
 * refus des valeurs par défaut committées sous profil {@code prod}.</p>
 */
public final class InternalSecretValidator {

    private static final Set<String> KNOWN_DEFAULTS = Set.of(
            "dev-internal-secret-change-me",
            "local-internal-secret-change-me",
            "change-me",
            "change-me-internal-secret-32chars",
            "test-internal-secret");

    private InternalSecretValidator() {
    }

    public static String requireValid(String secret, Environment environment) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "B3 : propriété 'app.internal-secret' absente ou vide — "
                            + "définir INTERNAL_SECRET au boot (échec volontaire, pas de défaut committé).");
        }
        if (isProd(environment) && KNOWN_DEFAULTS.contains(secret.trim())) {
            throw new IllegalStateException(
                    "B3 : 'app.internal-secret' vaut encore une valeur par défaut connue sous profil prod — "
                            + "définir un INTERNAL_SECRET fort et unique.");
        }
        return secret;
    }

    private static boolean isProd(Environment environment) {
        if (environment == null) {
            return false;
        }
        for (String profile : environment.getActiveProfiles()) {
            if ("prod".equalsIgnoreCase(profile)) {
                return true;
            }
        }
        return false;
    }
}
