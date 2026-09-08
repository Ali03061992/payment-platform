package com.paymentplatform.shared.infrastructure.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class JpaOutboxEventStoreTest {

    @Autowired
    private JpaOutboxEventStore store;

    @Autowired
    private OutboxEventRepository repository;

    @Test
    void append_savesEvent() {
        var event = new TestDomainEvent(UUID.randomUUID(), "payment.created", "agg-1");
        store.append(event, "agg-1");

        assertThat(repository.count()).isEqualTo(1);
        OutboxEventEntity saved = repository.findAll().get(0);
        assertThat(saved.getEventId()).isEqualTo(event.eventId().toString());
        assertThat(saved.getEventType()).isEqualTo("payment.created");
        assertThat(saved.getAggregateId()).isEqualTo("agg-1");
        assertThat(saved.getPayload()).contains("payment.created");
        assertThat(saved.getProcessedAt()).isNull();
    }

    @Test
    void append_multipleEvents() {
        store.append(new TestDomainEvent(UUID.randomUUID(), "payment.created", "agg-1"), "agg-1");
        store.append(new TestDomainEvent(UUID.randomUUID(), "order.created", "agg-2"), "agg-2");
        assertThat(repository.count()).isEqualTo(2);
    }

    record TestDomainEvent(UUID eventId, String eventType, String aggregateId) implements com.paymentplatform.shared.domain.event.DomainEvent {
        @Override public int eventVersion() { return 1; }
        @Override public Instant occurredAt() { return Instant.now(); }
    }
}
