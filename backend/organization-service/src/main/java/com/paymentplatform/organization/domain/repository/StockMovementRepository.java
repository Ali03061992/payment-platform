package com.paymentplatform.organization.domain.repository;

import com.paymentplatform.organization.domain.model.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    List<StockMovement> findByProductIdOrderByCreatedAtDesc(Long productId);
    List<StockMovement> findBySupplierIdOrderByCreatedAtDesc(Long supplierId);
}
