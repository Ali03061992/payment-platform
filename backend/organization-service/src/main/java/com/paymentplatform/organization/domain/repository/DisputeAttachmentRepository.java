package com.paymentplatform.organization.domain.repository;

import com.paymentplatform.organization.domain.model.DisputeAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DisputeAttachmentRepository extends JpaRepository<DisputeAttachment, UUID> {

    List<DisputeAttachment> findByDisputeIdOrderByCreatedAtAsc(UUID disputeId);
}
