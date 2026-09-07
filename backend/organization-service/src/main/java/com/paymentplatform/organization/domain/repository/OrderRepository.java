package com.paymentplatform.organization.domain.repository;

import com.paymentplatform.organization.domain.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    Page<Order> findBySupplierId(Long supplierId, Pageable pageable);

    Page<Order> findByShopId(Long shopId, Pageable pageable);

    Page<Order> findBySupplierIdAndStatus(Long supplierId, String status, Pageable pageable);

    Page<Order> findByShopIdAndStatus(Long shopId, String status, Pageable pageable);

    long countBySupplierId(Long supplierId);

    long countBySupplierIdAndStatus(Long supplierId, String status);

    long countByShopId(Long shopId);

    long countByShopIdAndStatus(Long shopId, String status);
}
