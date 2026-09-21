package com.paymentplatform.organization.domain.repository;

import com.paymentplatform.organization.domain.model.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    List<Product> findBySupplierIdAndStatus(UUID supplierId, String status);
    List<Product> findBySupplierId(UUID supplierId);
    Optional<Product> findBySupplierIdAndSku(UUID supplierId, String sku);
    boolean existsBySupplierIdAndSku(UUID supplierId, String sku);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(UUID id);

    @Query("SELECT p FROM Product p WHERE p.status = 'ACTIVE' AND (p.quantity - p.reservedQty) <= p.minQuantity")
    List<Product> findLowStockProducts();
}
