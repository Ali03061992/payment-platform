package com.paymentplatform.organization.domain.repository;

import com.paymentplatform.organization.domain.model.Dispute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DisputeRepository extends JpaRepository<Dispute, UUID> {

    List<Dispute> findByOrderIdOrderByCreatedAtDesc(UUID orderId);

    List<Dispute> findByShopIdOrderByCreatedAtDesc(UUID shopId);

    List<Dispute> findBySupplierIdOrderByCreatedAtDesc(UUID supplierId);

    List<Dispute> findByShopIdAndStatusOrderByCreatedAtDesc(UUID shopId, String status);

    List<Dispute> findBySupplierIdAndStatusOrderByCreatedAtDesc(UUID supplierId, String status);

    List<Dispute> findByStatusOrderByCreatedAtDesc(String status);
}
