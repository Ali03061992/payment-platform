package com.paymentplatform.organization.domain.repository;

import com.paymentplatform.organization.domain.model.DisputeNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DisputeNoteRepository extends JpaRepository<DisputeNote, UUID> {

    List<DisputeNote> findByDisputeIdOrderByCreatedAtAsc(UUID disputeId);
}
