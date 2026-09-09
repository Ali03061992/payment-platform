package com.paymentplatform.shared.infrastructure.outbox;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {

    List<OutboxEventEntity> findTop100ByProcessedAtIsNullOrderByIdAsc();
}