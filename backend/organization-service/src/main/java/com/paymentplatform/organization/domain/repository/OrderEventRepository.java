package com.paymentplatform.organization.domain.repository;

import com.paymentplatform.organization.domain.model.OrderEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderEventRepository extends JpaRepository<OrderEvent, UUID> {

    List<OrderEvent> findByOrderIdOrderByTimestampDesc(UUID orderId);
}
