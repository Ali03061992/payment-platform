package com.paymentplatform.organization.domain.repository;

import com.paymentplatform.organization.domain.model.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StockMovementRepository extends JpaRepository<StockMovement, UUID> {
    List<StockMovement> findByProductIdOrderByCreatedAtDesc(UUID productId);
    List<StockMovement> findBySupplierIdOrderByCreatedAtDesc(UUID supplierId);
}
