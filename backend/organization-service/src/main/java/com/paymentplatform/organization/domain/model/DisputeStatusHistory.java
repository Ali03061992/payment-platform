package com.paymentplatform.organization.domain.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** Journal d'audit des changements de statut d'un litige. */
@Entity
@Table(name = "dispute_status_history")
public class DisputeStatusHistory {

    @Id
    @Column(columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Column(name = "dispute_id", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID disputeId;

    @Column(name = "from_status", length = 30)
    private String fromStatus;

    @Column(name = "to_status", nullable = false, length = 30)
    private String toStatus;

    @Column(name = "changed_by", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID changedBy;

    @Column(length = 255)
    private String note;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    protected DisputeStatusHistory() {}



    public static DisputeStatusHistory record(UUID disputeId, String fromStatus, String toStatus,
                                              UUID changedBy, String note) {
        DisputeStatusHistory history = new DisputeStatusHistory();
        history.disputeId = disputeId;
        history.fromStatus = fromStatus;
        history.toStatus = toStatus;
        history.changedBy = changedBy;
        history.note = note;
        history.changedAt = Instant.now();
        return history;
    }

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (changedAt == null) changedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getDisputeId() { return disputeId; }
    public String getFromStatus() { return fromStatus; }
    public String getToStatus() { return toStatus; }
    public UUID getChangedBy() { return changedBy; }
    public String getNote() { return note; }
    public Instant getChangedAt() { return changedAt; }
}
