package com.paymentplatform.organization.domain.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** Piece jointe (PDF ou image) rattachee a un litige. */
@Entity
@Table(name = "dispute_attachments")
public class DisputeAttachment {

    @Id
    @Column(columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Column(name = "dispute_id", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID disputeId;

    @Column(name = "message_id", columnDefinition = "VARCHAR(36)")
    private UUID messageId;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "storage_path", nullable = false, length = 500)
    private String storagePath;

    @Column(name = "uploaded_by", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID uploadedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DisputeAttachment() {}

    public static DisputeAttachment create(UUID disputeId, UUID messageId, String fileName,
                                           String contentType, long sizeBytes, String storagePath,
                                           UUID uploadedBy) {
        DisputeAttachment attachment = new DisputeAttachment();
        attachment.disputeId = disputeId;
        attachment.messageId = messageId;
        attachment.fileName = fileName;
        attachment.contentType = contentType;
        attachment.sizeBytes = sizeBytes;
        attachment.storagePath = storagePath;
        attachment.uploadedBy = uploadedBy;
        attachment.createdAt = Instant.now();
        return attachment;
    }

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getDisputeId() { return disputeId; }
    public UUID getMessageId() { return messageId; }
    public String getFileName() { return fileName; }
    public String getContentType() { return contentType; }
    public long getSizeBytes() { return sizeBytes; }
    public String getStoragePath() { return storagePath; }
    public UUID getUploadedBy() { return uploadedBy; }
    public Instant getCreatedAt() { return createdAt; }
}
