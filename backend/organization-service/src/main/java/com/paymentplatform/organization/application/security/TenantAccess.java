package com.paymentplatform.organization.application.security;

import com.paymentplatform.shared.domain.exception.ForbiddenException;
import com.paymentplatform.shared.domain.model.RoleCode;
import com.paymentplatform.shared.domain.security.ActorContext;

import java.util.UUID;

/** Garde d'accès multi-tenant pour les objets métier fournisseur/boutique. */
public final class TenantAccess {

    private TenantAccess() {
    }

    public static void requireSupplier(ActorContext actor, UUID supplierId) {
        requireActor(actor);
        if (actor.isSystemAdmin()) {
            return;
        }
        requireOrganization(actor);
        if (!actor.belongsTo(supplierId)
                || !actor.hasAnyRole(RoleCode.SUPPLIER_ADMIN, RoleCode.SUPPLIER_AGENT)) {
            throw new ForbiddenException("Accès fournisseur non autorisé");
        }
    }

    public static void requireShop(ActorContext actor, UUID shopId) {
        requireActor(actor);
        if (actor.isSystemAdmin()) {
            return;
        }
        requireOrganization(actor);
        if (!actor.belongsTo(shopId)
                || !actor.hasAnyRole(RoleCode.SHOP_ADMIN, RoleCode.SHOP_AGENT)) {
            throw new ForbiddenException("Accès boutique non autorisé");
        }
    }

    public static void requireParticipant(ActorContext actor, UUID shopId, UUID supplierId,
                                          UUID deliveryAgentId) {
        requireActor(actor);
        if (actor.isSystemAdmin()) {
            return;
        }
        if (deliveryAgentId != null && deliveryAgentId.equals(actor.userId())) {
            return;
        }
        requireOrganization(actor);
        boolean supplierSide = actor.belongsTo(supplierId)
                && actor.hasAnyRole(RoleCode.SUPPLIER_ADMIN, RoleCode.SUPPLIER_AGENT);
        boolean shopSide = actor.belongsTo(shopId)
                && actor.hasAnyRole(RoleCode.SHOP_ADMIN, RoleCode.SHOP_AGENT);
        if (!supplierSide && !shopSide) {
            throw new ForbiddenException("Accès hors périmètre de la commande");
        }
    }

    public static void requireDeliveryActor(ActorContext actor, UUID supplierId, UUID deliveryAgentId) {
        requireActor(actor);
        if (actor.isSystemAdmin()) {
            return;
        }
        requireOrganization(actor);
        if (!actor.belongsTo(supplierId)) {
            throw new ForbiddenException("Accès livraison non autorisé");
        }
        if (actor.hasRole(RoleCode.SUPPLIER_ADMIN)) {
            return;
        }
        if (!actor.hasRole(RoleCode.SUPPLIER_AGENT)
                || deliveryAgentId == null
                || !deliveryAgentId.equals(actor.userId())) {
            throw new ForbiddenException("Seul le livreur assigné peut agir sur cette livraison");
        }
    }

    private static void requireActor(ActorContext actor) {
        if (actor == null) {
            throw new ForbiddenException("Acteur non authentifié");
        }
    }

    private static void requireOrganization(ActorContext actor) {
        if (actor.organizationId() == null) {
            throw new ForbiddenException("Aucune organisation rattachée à ce compte");
        }
    }
}
