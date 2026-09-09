package com.paymentplatform.organization.domain.repository;

import java.util.UUID;

import com.paymentplatform.organization.domain.model.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, UUID> {
    List<StockMovement> findByProductIdOrderByCreatedAtDesc(UUID productId);
    List<StockMovement> findBySupplierIdOrderByCreatedAtDesc(UUID supplierId);
}
