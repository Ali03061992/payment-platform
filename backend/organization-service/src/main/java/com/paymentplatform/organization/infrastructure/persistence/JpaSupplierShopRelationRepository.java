package com.paymentplatform.organization.infrastructure.persistence;

import com.paymentplatform.organization.domain.model.SupplierShopRelation;
import com.paymentplatform.organization.domain.repository.SupplierShopRelationRepository;
import com.paymentplatform.organization.domain.valueobject.OrganizationId;
import com.paymentplatform.organization.domain.valueobject.RelationStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class JpaSupplierShopRelationRepository implements SupplierShopRelationRepository {

    private final SupplierShopRelationJpaRepository jpa;

    public JpaSupplierShopRelationRepository(SupplierShopRelationJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SupplierShopRelation> findById(UUID id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierShopRelation> findBySupplierId(OrganizationId supplierId) {
        return jpa.findBySupplierId(supplierId.value()).stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierShopRelation> findByShopId(OrganizationId shopId) {
        return jpa.findByShopId(shopId.value()).stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierShopRelation> findByStatus(RelationStatus status) {
        return jpa.findByStatus(status.name()).stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierShopRelation> findBySupplierIdAndStatus(OrganizationId supplierId, RelationStatus status) {
        return jpa.findBySupplierIdAndStatus(supplierId.value(), status.name()).stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierShopRelation> findByShopIdAndStatus(OrganizationId shopId, RelationStatus status) {
        return jpa.findByShopIdAndStatus(shopId.value(), status.name()).stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsBySupplierIdAndShopId(OrganizationId supplierId, OrganizationId shopId) {
        return jpa.existsBySupplierIdAndShopId(supplierId.value(), shopId.value());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsBySupplierIdAndShopIdAndStatus(OrganizationId supplierId, OrganizationId shopId, RelationStatus status) {
        return jpa.existsBySupplierIdAndShopIdAndStatus(supplierId.value(), shopId.value(), status.name());
    }

    @Override
    @Transactional
    public SupplierShopRelation save(SupplierShopRelation relation) {
        SupplierShopRelationJpaEntity entity = toEntity(relation);
        SupplierShopRelationJpaEntity saved = jpa.save(entity);
        return toDomain(saved);
    }

    @Override
    @Transactional
    public void deactivateBySupplierId(OrganizationId supplierId) {
        jpa.deactivateBySupplierId(supplierId.value());
    }

    @Override
    @Transactional
    public void deactivateByShopId(OrganizationId shopId) {
        jpa.deactivateByShopId(shopId.value());
    }

    private SupplierShopRelationJpaEntity toEntity(SupplierShopRelation r) {
        SupplierShopRelationJpaEntity e = new SupplierShopRelationJpaEntity();
        if (r.id() != null) {
            e.setId(r.id());
        }
        e.setSupplierId(r.supplierId().value());
        e.setShopId(r.shopId().value());
        e.setStatus(r.status().name());
        e.setCreatedAt(r.createdAt());
        return e;
    }

    private SupplierShopRelation toDomain(SupplierShopRelationJpaEntity e) {
        return SupplierShopRelation.reconstruct(
                e.getId(),
                OrganizationId.of(e.getSupplierId()),
                OrganizationId.of(e.getShopId()),
                RelationStatus.valueOf(e.getStatus()),
                e.getCreatedAt());
    }
}
