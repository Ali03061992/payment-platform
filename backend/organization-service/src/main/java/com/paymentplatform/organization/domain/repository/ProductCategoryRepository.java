package com.paymentplatform.organization.domain.repository;

import com.paymentplatform.organization.domain.model.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {
    List<ProductCategory> findBySupplierId(Long supplierId);
    Optional<ProductCategory> findBySupplierIdAndCode(Long supplierId, String code);
    boolean existsBySupplierIdAndCode(Long supplierId, String code);
}
