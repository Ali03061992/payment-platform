package com.paymentplatform.organization.domain.repository;

import com.paymentplatform.organization.domain.model.ProductFamily;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductFamilyRepository extends JpaRepository<ProductFamily, UUID> {
    /** M3 : les lignes soft-deletées sont exclues des listes. */
    List<ProductFamily> findBySupplierIdAndDeletedAtIsNull(UUID supplierId);

    @Query("SELECT f FROM ProductFamily f JOIN f.categories c"
            + " WHERE c.id = :categoryId AND f.deletedAt IS NULL AND c.deletedAt IS NULL")
    List<ProductFamily> findByCategoryId(@Param("categoryId") UUID categoryId);

    Optional<ProductFamily> findBySupplierIdAndCode(UUID supplierId, String code);
}
