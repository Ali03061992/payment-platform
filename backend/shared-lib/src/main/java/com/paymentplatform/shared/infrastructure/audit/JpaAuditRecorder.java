package com.paymentplatform.shared.infrastructure.audit;

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
    public void record(Long userId, Long organizationId, String action, Long entityId, String details) {
        repository.save(new AuditLogEntity(userId, organizationId, action, entityId, details));
    }
}