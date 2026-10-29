package com.paymentplatform.shared.domain.security;

import com.paymentplatform.shared.domain.model.RoleCode;

import java.util.List;
import java.util.UUID;

/**
 * Contexte de l'acteur qui déclenche une action métier.
 * Il porte l'identité et le périmètre organisationnel, indépendamment du web.
 */
public record ActorContext(UUID userId, UUID organizationId, List<String> roles) {

    public ActorContext {
        roles = roles == null ? List.of() : List.copyOf(roles);
    }

    public static ActorContext system() {
        return new ActorContext(null, null, List.of(RoleCode.SYSTEM_ADMIN.name()));
    }

    public static ActorContext system(UUID actorUserId) {
        return new ActorContext(actorUserId, null, List.of(RoleCode.SYSTEM_ADMIN.name()));
    }

    public boolean isSystemAdmin() {
        return hasRole(RoleCode.SYSTEM_ADMIN);
    }

    public boolean hasRole(RoleCode role) {
        return role != null && roles.contains(role.name());
    }

    public boolean hasAnyRole(RoleCode... expected) {
        if (expected == null) {
            return false;
        }
        for (RoleCode role : expected) {
            if (hasRole(role)) {
                return true;
            }
        }
        return false;
    }

    public boolean belongsTo(UUID organizationId) {
        return organizationId != null && organizationId.equals(this.organizationId);
    }
}
