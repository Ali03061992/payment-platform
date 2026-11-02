package com.paymentplatform.organization.domain.repository;

import com.paymentplatform.organization.domain.model.DisputeStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DisputeStatusHistoryRepository extends JpaRepository<DisputeStatusHistory, UUID> {

    List<DisputeStatusHistory> findByDisputeIdOrderByChangedAtAsc(UUID disputeId);
}
