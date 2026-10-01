package com.paymentplatform.shared.infrastructure.eventing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;

/**
 * Idempotence des consommateurs : marque un événement comme traité.
 * Retourne false si l'événement a déjà été traité (unicité event_id).
 */
@Component
public class EventDeduplicator {

    private static final Logger log = LoggerFactory.getLogger(EventDeduplicator.class);

    private final ProcessedEventRepository repository;
    private final JdbcClient jdbcClient;

    public EventDeduplicator(ProcessedEventRepository repository, JdbcClient jdbcClient) {
        this.repository = repository;
        this.jdbcClient = jdbcClient;
    }

    @Transactional(readOnly = true)
    public boolean isProcessed(String eventId) {
        return repository.existsById(eventId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean markProcessed(String eventId) {
        try {
            // INSERT direct via Spring JDBC : un doublon lève une violation de clé
            // (save() JPA ferait un merge silencieux et ne détecterait jamais le doublon).
            jdbcClient.sql("INSERT INTO processed_events(event_id, processed_at) VALUES(:id, :at)")
                    .param("id", eventId)
                    .param("at", Timestamp.from(Instant.now()))
                    .update();
            return true;
        } catch (DataIntegrityViolationException e) {
            log.debug("Événement {} déjà traité (idempotence)", eventId);
            return false;
        }
    }
}