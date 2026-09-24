package com.paymentplatform.organization.domain.repository;

import com.paymentplatform.organization.domain.model.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, UUID> {
    /** M3 : les lignes soft-deletées sont exclues des listes. */
    List<ProductCategory> findBySupplierIdAndDeletedAtIsNull(UUID supplierId);
    Optional<ProductCategory> findBySupplierIdAndCodeAndDeletedAtIsNull(UUID supplierId, String code);
    boolean existsBySupplierIdAndCodeAndDeletedAtIsNull(UUID supplierId, String code);
}
