package com.paymentplatform.shared.infrastructure.outbox;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OutboxEventRepositoryTest {

    @Autowired
    private OutboxEventRepository repository;

    private OutboxEventEntity createTestEvent(String eventType) {
        var event = new TestDomainEvent(UUID.randomUUID(), eventType, "agg-1");
        return new OutboxEventEntity(event, "{\"type\":\"" + eventType + "\"}");
    }

    @Test
    void save_andFindById() {
        OutboxEventEntity saved = repository.save(createTestEvent("payment.created"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEventId()).isNotNull();
        assertThat(saved.getEventType()).isEqualTo("payment.created");
        assertThat(saved.getProcessedAt()).isNull();
    }

    @Test
    void findTop100ByProcessedAtIsNullOrderByIdAsc_returnsUnprocessed() {
        repository.save(createTestEvent("payment.created"));

        OutboxEventEntity processed = repository.save(createTestEvent("order.created"));
        processed.markProcessed();
        repository.save(processed);

        var pending = repository.findTop100ByProcessedAtIsNullOrderByIdAsc();
        assertThat(pending).hasSize(1);
        assertThat(pending.get(0).getEventType()).isEqualTo("payment.created");
    }

    @Test
    void findTop100ByProcessedAtIsNullOrderByIdAsc_maxResults() {
        for (int i = 0; i < 105; i++) {
            repository.save(createTestEvent("payment.created"));
        }
        var pending = repository.findTop100ByProcessedAtIsNullOrderByIdAsc();
        assertThat(pending).hasSize(100);
    }

    @Test
    void markProcessed_setsProcessedAt() {
        OutboxEventEntity saved = repository.save(createTestEvent("payment.created"));

        saved.markProcessed();
        repository.save(saved);

        var found = repository.findById(saved.getId()).orElseThrow();
        assertThat(found.getProcessedAt()).isNotNull();
    }

    record TestDomainEvent(UUID eventId, String eventType, String aggregateId) implements com.paymentplatform.shared.domain.event.DomainEvent {
        @Override public int eventVersion() { return 1; }
        @Override public Instant occurredAt() { return Instant.now(); }
    }
}
