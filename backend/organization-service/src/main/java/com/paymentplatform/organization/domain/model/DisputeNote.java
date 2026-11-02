package com.paymentplatform.organization.domain.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** Note interne visible uniquement par les administrateurs du litige. */
@Entity
@Table(name = "dispute_notes")
public class DisputeNote {

    @Id
    @Column(columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Column(name = "dispute_id", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID disputeId;

    @Column(name = "author_id", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID authorId;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DisputeNote() {}

    public static DisputeNote create(UUID disputeId, UUID authorId, String content) {
        DisputeNote note = new DisputeNote();
        note.disputeId = disputeId;
        note.authorId = authorId;
        note.content = content;
        note.createdAt = Instant.now();
        return note;
    }

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getDisputeId() { return disputeId; }
    public UUID getAuthorId() { return authorId; }
    public String getContent() { return content; }
    public Instant getCreatedAt() { return createdAt; }
}
