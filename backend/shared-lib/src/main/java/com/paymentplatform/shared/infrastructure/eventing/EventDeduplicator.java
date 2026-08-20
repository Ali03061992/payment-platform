package com.paymentplatform.shared.infrastructure.eventing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Idempotence des consommateurs : marque un événement comme traité.
 * Retourne false si l'événement a déjà été traité (unicité event_id).
 */
@Component
public class EventDeduplicator {

    private static final Logger log = LoggerFactory.getLogger(EventDeduplicator.class);

    private final ProcessedEventRepository repository;

    public EventDeduplicator(ProcessedEventRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public boolean isProcessed(String eventId) {
        return repository.existsById(eventId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean markProcessed(String eventId) {
        try {
            repository.save(new ProcessedEventEntity(eventId));
            repository.flush();
            return true;
        } catch (DataIntegrityViolationException e) {
            log.debug("Événement {} déjà traité (idempotence)", eventId);
            return false;
        }
    }
}