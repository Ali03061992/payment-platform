package com.paymentplatform.organization.domain.repository;

import com.paymentplatform.organization.domain.model.ProductFamily;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProductFamilyRepository extends JpaRepository<ProductFamily, Long> {
    List<ProductFamily> findBySupplierId(Long supplierId);
    List<ProductFamily> findByCategoryId(Long categoryId);
    Optional<ProductFamily> findBySupplierIdAndCode(Long supplierId, String code);
}
