package com.paymentplatform.shared.infrastructure.audit;

import java.util.UUID;

/** Enregistre une entrée d'audit dans la transaction courante. */
public interface AuditRecorder {

    void record(UUID userId, UUID organizationId, String action, UUID entityId, String details);
}