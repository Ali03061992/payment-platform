package com.paymentplatform.shared.infrastructure.audit;

/** Enregistre une entrée d'audit dans la transaction courante. */
public interface AuditRecorder {

    void record(Long userId, Long organizationId, String action, Long entityId, String details);
}