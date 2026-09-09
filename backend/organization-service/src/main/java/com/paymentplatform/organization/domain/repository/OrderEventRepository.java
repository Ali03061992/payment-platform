package com.paymentplatform.organization.domain.repository;

import java.util.UUID;

import com.paymentplatform.organization.domain.model.OrderEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderEventRepository extends JpaRepository<OrderEvent, UUID> {

    List<OrderEvent> findByOrderIdOrderByTimestampDesc(UUID orderId);
}
