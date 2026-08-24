package com.paymentplatform.organization.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface SupplierShopRelationJpaRepository extends JpaRepository<SupplierShopRelationJpaEntity, Long> {

    List<SupplierShopRelationJpaEntity> findBySupplierId(Long supplierId);
    List<SupplierShopRelationJpaEntity> findByShopId(Long shopId);
    List<SupplierShopRelationJpaEntity> findByStatus(String status);
    List<SupplierShopRelationJpaEntity> findBySupplierIdAndStatus(Long supplierId, String status);
    List<SupplierShopRelationJpaEntity> findByShopIdAndStatus(Long shopId, String status);
    boolean existsBySupplierIdAndShopId(Long supplierId, Long shopId);
    boolean existsBySupplierIdAndShopIdAndStatus(Long supplierId, Long shopId, String status);

    @Modifying
    @Transactional
    @Query("UPDATE SupplierShopRelationJpaEntity r SET r.status = 'INACTIVE' WHERE r.supplierId = :supplierId")
    void deactivateBySupplierId(Long supplierId);

    @Modifying
    @Transactional
    @Query("UPDATE SupplierShopRelationJpaEntity r SET r.status = 'INACTIVE' WHERE r.shopId = :shopId")
    void deactivateByShopId(Long shopId);
}
