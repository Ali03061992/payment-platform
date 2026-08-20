package com.paymentplatform.shared.infrastructure.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Entrée d'audit (table audit_logs, présente dans chaque base). */
@Entity
@Table(name = "audit_logs")
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "organization_id")
    private Long organizationId;

    @Column(nullable = false, length = 60)
    private String action;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(columnDefinition = "TEXT")
    private String details;

    protected AuditLogEntity() {
    }

    public AuditLogEntity(Long userId, Long organizationId, String action, Long entityId, String details) {
        this.userId = userId;
        this.organizationId = organizationId;
        this.action = action;
        this.entityId = entityId;
        this.timestamp = Instant.now();
        this.details = details;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public String getAction() {
        return action;
    }

    public Long getEntityId() {
        return entityId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getDetails() {
        return details;
    }
}