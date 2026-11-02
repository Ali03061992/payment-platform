package com.paymentplatform.organization.domain.model;

import com.paymentplatform.shared.domain.exception.ConflictException;

import java.time.Duration;
import java.time.Instant;

/** Priorite d'un litige et delai de traitement (SLA) associe. */
public enum DisputePriority {

    LOW(Duration.ofDays(7)),
    NORMAL(Duration.ofHours(72)),
    HIGH(Duration.ofHours(24)),
    URGENT(Duration.ofHours(4));

    private final Duration sla;

    DisputePriority(Duration sla) {
        this.sla = sla;
    }

    public Duration sla() {
        return sla;
    }

    public Instant dueAt(Instant from) {
        return from.plus(sla);
    }

    public static DisputePriority from(String value) {
        if (value == null || value.isBlank()) {
            return NORMAL;
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ConflictException("Priorite de litige invalide : " + value);
        }
    }
}
