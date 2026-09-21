package com.paymentplatform.organization.domain.repository;

import com.paymentplatform.organization.domain.model.SupplierShopRelation;
import com.paymentplatform.organization.domain.valueobject.OrganizationId;
import com.paymentplatform.organization.domain.valueobject.RelationStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SupplierShopRelationRepository {
    Optional<SupplierShopRelation> findById(UUID id);
    List<SupplierShopRelation> findBySupplierId(OrganizationId supplierId);
    List<SupplierShopRelation> findByShopId(OrganizationId shopId);
    List<SupplierShopRelation> findByStatus(RelationStatus status);
    List<SupplierShopRelation> findBySupplierIdAndStatus(OrganizationId supplierId, RelationStatus status);
    List<SupplierShopRelation> findByShopIdAndStatus(OrganizationId shopId, RelationStatus status);
    boolean existsBySupplierIdAndShopId(OrganizationId supplierId, OrganizationId shopId);
    boolean existsBySupplierIdAndShopIdAndStatus(OrganizationId supplierId, OrganizationId shopId, RelationStatus status);
    SupplierShopRelation save(SupplierShopRelation relation);
    void deactivateBySupplierId(OrganizationId supplierId);
    void deactivateByShopId(OrganizationId shopId);
}
