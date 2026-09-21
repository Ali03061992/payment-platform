package com.paymentplatform.organization.domain.repository;

import com.paymentplatform.organization.domain.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByReference(String reference);

    List<Order> findBySupplierId(UUID supplierId);

    List<Order> findByShopId(UUID shopId);

    List<Order> findBySupplierIdAndStatus(UUID supplierId, String status);

    List<Order> findByShopIdAndStatus(UUID shopId, String status);

    List<Order> findByDeliveryAgentId(UUID deliveryAgentId);

    Page<Order> findBySupplierId(UUID supplierId, Pageable pageable);

    Page<Order> findByShopId(UUID shopId, Pageable pageable);

    Page<Order> findBySupplierIdAndStatus(UUID supplierId, String status, Pageable pageable);

    Page<Order> findByShopIdAndStatus(UUID shopId, String status, Pageable pageable);

    UUID countBySupplierId(UUID supplierId);

    long countBySupplierIdAndStatus(UUID supplierId, String status);

    UUID countByShopId(UUID shopId);

    long countByShopIdAndStatus(UUID shopId, String status);
}
