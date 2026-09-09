package com.paymentplatform.shared.infrastructure.audit;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JpaAuditRecorder implements AuditRecorder {

    private final AuditLogRepository repository;

    public JpaAuditRecorder(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void record(UUID userId, UUID organizationId, String action, UUID entityId, String details) {
        repository.save(new AuditLogEntity(userId, organizationId, action, entityId, details));
    }
}