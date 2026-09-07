package com.paymentplatform.organization.domain.repository;

import com.paymentplatform.organization.domain.model.ProductFamily;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ProductFamilyRepository extends JpaRepository<ProductFamily, Long> {
    List<ProductFamily> findBySupplierId(Long supplierId);

    @Query("SELECT f FROM ProductFamily f JOIN f.categories c WHERE c.id = :categoryId")
    List<ProductFamily> findByCategoryId(@Param("categoryId") Long categoryId);

    Optional<ProductFamily> findBySupplierIdAndCode(Long supplierId, String code);
}
