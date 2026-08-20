package com.paymentplatform.shared.infrastructure.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Relais de l'outbox : publie périodiquement les événements non traités sur
 * RabbitMQ puis les marque traités. Le consommateur est idempotent (eventId),
 * donc un crash entre publication et marquage est sans conséquence.
 */
@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
    private static final int BATCH_SIZE = 100;

    private final OutboxEventRepository repository;
    private final OutboxPublisher publisher;
    private final AmqpTopology topology;

    public OutboxRelay(OutboxEventRepository repository, OutboxPublisher publisher, AmqpTopology topology) {
        this.repository = repository;
        this.publisher = publisher;
        this.topology = topology;
    }

    @Scheduled(fixedDelayString = "${app.outbox.relay-interval-ms:1000}")
    @Transactional
    public void relay() {
        List<OutboxEventEntity> pending = repository.findTop100ByProcessedAtIsNullOrderByIdAsc();
        if (pending.isEmpty()) {
            return;
        }
        for (OutboxEventEntity event : pending) {
            try {
                publisher.publish(topology.exchangeFor(event.getEventType()), event.getEventType(),
                        event.getEventId(), event.getPayload());
                event.markProcessed();
            } catch (Exception e) {
                log.error("Échec de publication de l'événement {} (id={}), nouvelle tentative au prochain tick",
                        event.getEventType(), event.getEventId(), e);
            }
        }
    }
}