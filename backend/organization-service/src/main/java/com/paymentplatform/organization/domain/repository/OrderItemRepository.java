package com.paymentplatform.organization.domain.repository;

import java.util.UUID;

import com.paymentplatform.organization.domain.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    List<OrderItem> findByOrderId(UUID orderId);
}
