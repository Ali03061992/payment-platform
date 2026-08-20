package com.paymentplatform.shared.infrastructure.security;

import java.util.List;

/**
 * Utilisateur authentifié extrait du JWT.
 * Le JWT porte uniquement : userId, username, roles, organizationId.
 */
public record AuthenticatedUser(long userId, String username, List<String> roles, Long organizationId) {

    public static final String ROLE_PREFIX = "ROLE_";
}