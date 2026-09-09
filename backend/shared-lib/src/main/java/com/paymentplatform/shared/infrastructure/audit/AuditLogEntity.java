package com.paymentplatform.shared.infrastructure.audit;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import org.hibernate.annotations.UuidGenerator;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Entrée d'audit (table audit_logs, présente dans chaque base). */
@Entity
@Table(name = "audit_logs")
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @UuidGenerator(style = UuidGenerator.Style.RANDOM)
    @Column(columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Column(name = "user_id", columnDefinition = "VARCHAR(36)")
    private UUID userId;

    @Column(name = "organization_id", columnDefinition = "VARCHAR(36)")
    private UUID organizationId;

    @Column(nullable = false, length = 60)
    private String action;

    @Column(name = "entity_id", columnDefinition = "VARCHAR(36)")
    private UUID entityId;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(columnDefinition = "TEXT")
    private String details;

    protected AuditLogEntity() {
    }

    public AuditLogEntity(UUID userId, UUID organizationId, String action, UUID entityId, String details) {
        this.userId = userId;
        this.organizationId = organizationId;
        this.action = action;
        this.entityId = entityId;
        this.timestamp = Instant.now();
        this.details = details;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public String getAction() {
        return action;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getDetails() {
        return details;
    }
}