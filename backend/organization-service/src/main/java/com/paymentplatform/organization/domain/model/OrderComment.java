package com.paymentplatform.organization.domain.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "order_comments")
public class OrderComment {

    @Id
    @Column(columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Column(name = "order_id", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID orderId;

    @Column(name = "author_id", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID authorId;

    @Column(name = "author_name", nullable = false, length = 200)
    private String authorName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (this.id == null) { this.id = java.util.UUID.randomUUID(); }
        if (createdAt == null) createdAt = Instant.now();
    }

    public static OrderComment create(UUID orderId, UUID authorId, String authorName, String content) {
        OrderComment comment = new OrderComment();
        comment.orderId = orderId;
        comment.authorId = authorId;
        comment.authorName = authorName;
        comment.content = content;
        return comment;
    }

    public UUID getId() { return id; }
    public UUID getOrderId() { return orderId; }
    public UUID getAuthorId() { return authorId; }
    public String getAuthorName() { return authorName; }
    public String getContent() { return content; }
    public Instant getCreatedAt() { return createdAt; }
}
