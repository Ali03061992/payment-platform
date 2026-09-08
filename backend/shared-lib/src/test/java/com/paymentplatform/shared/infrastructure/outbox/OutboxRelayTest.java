package com.paymentplatform.shared.infrastructure.outbox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OutboxRelayTest {

    @Autowired
    private OutboxRelay relay;

    @Autowired
    private OutboxEventRepository repository;

    @Autowired
    private OutboxPublisher publisher;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    private OutboxEventEntity createEvent(String eventType) {
        var event = new TestDomainEvent(UUID.randomUUID(), eventType, "agg");
        return new OutboxEventEntity(event, "{}");
    }

    @Test
    void relay_noEvents_doesNothing() {
        relay.relay();
        assertThat(repository.count()).isEqualTo(0);
    }

    @Test
    void relay_alreadyProcessed_ignores() {
        OutboxEventEntity e = repository.save(createEvent("payment.created"));
        e.markProcessed();
        repository.save(e);

        relay.relay();

        assertThat(repository.findTop100ByProcessedAtIsNullOrderByIdAsc()).isEmpty();
    }

    record TestDomainEvent(UUID eventId, String eventType, String aggregateId) implements com.paymentplatform.shared.domain.event.DomainEvent {
        @Override public int eventVersion() { return 1; }
        @Override public Instant occurredAt() { return Instant.now(); }
    }
}
