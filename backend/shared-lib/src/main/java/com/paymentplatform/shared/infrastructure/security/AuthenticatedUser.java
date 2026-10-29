package com.paymentplatform.shared.infrastructure.security;

import com.paymentplatform.shared.domain.security.ActorContext;

import java.util.List;
import java.util.UUID;

/**
 * Utilisateur authentifié extrait du JWT.
 * Le JWT porte uniquement : userId, username, roles, organizationId.
 */
public record AuthenticatedUser(UUID userId, String username, List<String> roles, UUID organizationId) {

    public static final String ROLE_PREFIX = "ROLE_";

    public ActorContext actor() {
        return new ActorContext(userId, organizationId, roles);
    }
}