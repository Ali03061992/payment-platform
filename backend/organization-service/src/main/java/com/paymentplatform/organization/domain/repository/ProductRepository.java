package com.paymentplatform.organization.domain.repository;

import com.paymentplatform.organization.domain.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findBySupplierIdAndStatus(Long supplierId, String status);
    List<Product> findBySupplierId(Long supplierId);
    Optional<Product> findBySupplierIdAndSku(Long supplierId, String sku);
    boolean existsBySupplierIdAndSku(Long supplierId, String sku);
}
