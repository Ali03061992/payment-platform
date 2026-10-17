package com.paymentplatform.notification.domain.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** Paire de clés VAPID générée automatiquement (fallback quand aucune n'est fournie). */
@Entity
@Table(name = "vapid_keypairs")
public class VapidKeyPair {

    @Id
    @Column(columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Column(name = "public_key", nullable = false)
    private String publicKey;

    @Column(name = "private_key", nullable = false)
    private String privateKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected VapidKeyPair() {}

    public VapidKeyPair(String publicKey, String privateKey) {
        this.id = UUID.randomUUID();
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.createdAt = Instant.now();
    }

    public UUID id() { return id; }
    public String publicKey() { return publicKey; }
    public String privateKey() { return privateKey; }
    public Instant createdAt() { return createdAt; }
}
