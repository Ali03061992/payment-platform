package com.paymentplatform.organization.domain.repository;

import com.paymentplatform.organization.domain.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByReference(String reference);

    List<Order> findBySupplierId(Long supplierId);

    List<Order> findByShopId(Long shopId);

    List<Order> findBySupplierIdAndStatus(Long supplierId, String status);

    List<Order> findByShopIdAndStatus(Long shopId, String status);

    List<Order> findByDeliveryAgentId(Long deliveryAgentId);

    long countBySupplierId(Long supplierId);

    long countBySupplierIdAndStatus(Long supplierId, String status);

    long countByShopId(Long shopId);

    long countByShopIdAndStatus(Long shopId, String status);
}
