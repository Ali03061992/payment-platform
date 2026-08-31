package com.paymentplatform.organization.domain.repository;

import com.paymentplatform.organization.domain.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findBySupplierIdAndStatus(Long supplierId, String status);
    List<Product> findBySupplierId(Long supplierId);
    Optional<Product> findBySupplierIdAndSku(Long supplierId, String sku);
    boolean existsBySupplierIdAndSku(Long supplierId, String sku);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(Long id);
}
