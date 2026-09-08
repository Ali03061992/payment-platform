package com.paymentplatform.shared.infrastructure.eventing;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ProcessedEventRepositoryTest {

    @Autowired
    private ProcessedEventRepository repository;

    @Test
    void save_andFindById() {
        String eventId = "evt-repo-" + System.nanoTime();
        ProcessedEventEntity entity = new ProcessedEventEntity(eventId);
        repository.save(entity);

        var found = repository.findById(eventId);
        assertThat(found).isPresent();
        assertThat(found.get().getEventId()).isEqualTo(eventId);
        assertThat(found.get().getProcessedAt()).isNotNull();
    }

    @Test
    void existsById_afterSave() {
        String eventId = "evt-repo-exists-" + System.nanoTime();
        repository.save(new ProcessedEventEntity(eventId));
        assertThat(repository.existsById(eventId)).isTrue();
    }

    @Test
    void existsById_unknownEvent() {
        assertThat(repository.existsById("evt-nonexistent-" + System.nanoTime())).isFalse();
    }

    @Test
    void count_works() {
        long before = repository.count();
        repository.save(new ProcessedEventEntity("evt-count-" + System.nanoTime()));
        repository.save(new ProcessedEventEntity("evt-count-" + (System.nanoTime() + 1)));
        assertThat(repository.count()).isEqualTo(before + 2);
    }
}
