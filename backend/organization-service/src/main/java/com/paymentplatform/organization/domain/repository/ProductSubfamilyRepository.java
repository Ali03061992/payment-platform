package com.paymentplatform.organization.domain.repository;

import com.paymentplatform.organization.domain.model.ProductSubfamily;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductSubfamilyRepository extends JpaRepository<ProductSubfamily, Long> {
    List<ProductSubfamily> findBySupplierId(Long supplierId);
    List<ProductSubfamily> findByFamilyId(Long familyId);
}
