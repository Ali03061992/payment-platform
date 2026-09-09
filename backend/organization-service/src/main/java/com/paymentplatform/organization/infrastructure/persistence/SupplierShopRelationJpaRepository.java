package com.paymentplatform.organization.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface SupplierShopRelationJpaRepository extends JpaRepository<SupplierShopRelationJpaEntity, UUID> {

    List<SupplierShopRelationJpaEntity> findBySupplierId(UUID supplierId);
    List<SupplierShopRelationJpaEntity> findByShopId(UUID shopId);
    List<SupplierShopRelationJpaEntity> findByStatus(String status);
    List<SupplierShopRelationJpaEntity> findBySupplierIdAndStatus(UUID supplierId, String status);
    List<SupplierShopRelationJpaEntity> findByShopIdAndStatus(UUID shopId, String status);
    boolean existsBySupplierIdAndShopId(UUID supplierId, UUID shopId);
    boolean existsBySupplierIdAndShopIdAndStatus(UUID supplierId, UUID shopId, String status);

    @Modifying
    @Transactional
    @Query("UPDATE SupplierShopRelationJpaEntity r SET r.status = 'INACTIVE' WHERE r.supplierId = :supplierId")
    void deactivateBySupplierId(UUID supplierId);

    @Modifying
    @Transactional
    @Query("UPDATE SupplierShopRelationJpaEntity r SET r.status = 'INACTIVE' WHERE r.shopId = :shopId")
    void deactivateByShopId(UUID shopId);
}
