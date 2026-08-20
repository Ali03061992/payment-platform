package com.paymentplatform.shared.infrastructure.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentplatform.shared.domain.event.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Port d'émission des événements : écrit la ligne d'outbox dans la transaction
 * courante (transaction locale couvrant agrégat + outbox).
 */
public interface OutboxEventStore {

    void append(DomainEvent event, String aggregateId);
}

@Component
class JpaOutboxEventStore implements OutboxEventStore {

    private static final Logger log = LoggerFactory.getLogger(JpaOutboxEventStore.class);

    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;

    JpaOutboxEventStore(OutboxEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void append(DomainEvent event, String aggregateId) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            repository.save(new OutboxEventEntity(event, payload));
        } catch (JsonProcessingException e) {
            log.error("Serialization impossible de l'événement {}", event.eventType(), e);
            throw new IllegalStateException("Impossible de sérialiser l'événement " + event.eventType(), e);
        }
    }
}