package com.paymentplatform.organization.domain.repository;

import com.paymentplatform.organization.domain.model.OrderComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderCommentRepository extends JpaRepository<OrderComment, UUID> {

    List<OrderComment> findByOrderIdOrderByCreatedAtAsc(UUID orderId);
}
