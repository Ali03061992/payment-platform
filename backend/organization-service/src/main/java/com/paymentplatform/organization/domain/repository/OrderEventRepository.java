package com.paymentplatform.organization.domain.repository;

import com.paymentplatform.organization.domain.model.OrderEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderEventRepository extends JpaRepository<OrderEvent, Long> {

    List<OrderEvent> findByOrderIdOrderByTimestampDesc(Long orderId);
}
