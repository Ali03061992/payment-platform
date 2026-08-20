package com.paymentplatform.shared.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Contrat commun des Domain Events du platform.
 * Chaque événement est un record sérialisable en JSON (Jackson) et transporte
 * eventId (idempotence), occurredAt, eventVersion (versionnement de contrat)
 * et aggregateId (référence à la racine d'agrégat émettante).
 */
public interface DomainEvent {

    String eventType();

    int eventVersion();

    UUID eventId();

    Instant occurredAt();

    String aggregateId();
}