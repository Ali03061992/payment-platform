package com.paymentplatform.organization.domain.repository;

import java.util.UUID;

import com.paymentplatform.organization.domain.model.ProductFamily;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ProductFamilyRepository extends JpaRepository<ProductFamily, UUID> {
    List<ProductFamily> findBySupplierId(UUID supplierId);

    @Query("SELECT f FROM ProductFamily f JOIN f.categories c WHERE c.id = :categoryId")
    List<ProductFamily> findByCategoryId(@Param("categoryId") UUID categoryId);

    Optional<ProductFamily> findBySupplierIdAndCode(UUID supplierId, String code);
}
