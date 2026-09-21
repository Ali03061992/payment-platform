package com.paymentplatform.organization.domain.repository;

import com.paymentplatform.organization.domain.model.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, UUID> {
    List<ProductCategory> findBySupplierId(UUID supplierId);
    Optional<ProductCategory> findBySupplierIdAndCode(UUID supplierId, String code);
    boolean existsBySupplierIdAndCode(UUID supplierId, String code);
}
