package com.paymentplatform.organization.domain.model;

import com.paymentplatform.shared.domain.exception.DomainException;
import com.paymentplatform.organization.domain.valueobject.OrganizationId;
import com.paymentplatform.organization.domain.valueobject.RelationStatus;

import java.time.Instant;

/**
 * Entity representing a relation between a supplier and a shop.
 */
public class SupplierShopRelation {

    private Long id;
    private final OrganizationId supplierId;
    private final OrganizationId shopId;
    private RelationStatus status;
    private final Instant createdAt;

    private SupplierShopRelation(Long id, OrganizationId supplierId, OrganizationId shopId,
                                 RelationStatus status, Instant createdAt) {
        this.id = id;
        this.supplierId = supplierId;
        this.shopId = shopId;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static SupplierShopRelation create(OrganizationId supplierId, OrganizationId shopId) {
        if (supplierId == null || shopId == null) {
            throw new DomainException("Les IDs fournisseur et boutique sont requis");
        }
        if (supplierId.value() == shopId.value()) {
            throw new DomainException("Un fournisseur ne peut pas être associé à lui-même");
        }
        return new SupplierShopRelation(null, supplierId, shopId, RelationStatus.ACTIVE, Instant.now());
    }

    public static SupplierShopRelation reconstruct(Long id, OrganizationId supplierId, OrganizationId shopId,
                                                    RelationStatus status, Instant createdAt) {
        return new SupplierShopRelation(id, supplierId, shopId, status, createdAt);
    }

    public void deactivate() {
        this.status = RelationStatus.INACTIVE;
    }

    public void activate() {
        this.status = RelationStatus.ACTIVE;
    }

    public boolean isActive() {
        return status == RelationStatus.ACTIVE;
    }

    public Long id() { return id; }
    public void setId(Long id) { this.id = id; }
    public OrganizationId supplierId() { return supplierId; }
    public OrganizationId shopId() { return shopId; }
    public RelationStatus status() { return status; }
    public Instant createdAt() { return createdAt; }
}
