package com.paymentplatform.notification.domain.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** Préférences de notification par utilisateur (opt-out explicite possible). */
@Entity
@Table(name = "notification_preferences", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id"})
})
public class NotificationPreference {

    @Id
    @Column(columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Column(name = "user_id", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID userId;

    @Column(name = "push_enabled", nullable = false)
    private boolean pushEnabled;

    @Column(name = "email_enabled", nullable = false)
    private boolean emailEnabled;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected NotificationPreference() {}

    public NotificationPreference(UUID userId) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.pushEnabled = true;
        this.emailEnabled = true;
        this.createdAt = Instant.now();
    }

    public void update(boolean pushEnabled, boolean emailEnabled) {
        this.pushEnabled = pushEnabled;
        this.emailEnabled = emailEnabled;
        this.updatedAt = Instant.now();
    }

    public UUID id() { return id; }
    public UUID userId() { return userId; }
    public boolean pushEnabled() { return pushEnabled; }
    public boolean emailEnabled() { return emailEnabled; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
