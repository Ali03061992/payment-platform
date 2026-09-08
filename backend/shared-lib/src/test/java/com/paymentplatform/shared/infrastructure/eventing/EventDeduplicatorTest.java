package com.paymentplatform.shared.infrastructure.eventing;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class EventDeduplicatorTest {

    private final ProcessedEventRepository repository = mock(ProcessedEventRepository.class);
    private final EventDeduplicator deduplicator = new EventDeduplicator(repository);

    @Test
    void markProcessed_returnsTrue_firstTime() {
        when(repository.save(any())).thenReturn(new ProcessedEventEntity("evt-1"));

        assertThat(deduplicator.markProcessed("evt-1")).isTrue();
        verify(repository).flush();
    }

    @Test
    void markProcessed_returnsFalse_whenDuplicate() {
        when(repository.save(any())).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThat(deduplicator.markProcessed("evt-dup")).isFalse();
    }

    @Test
    void isProcessed_delegatesToRepository() {
        when(repository.existsById("evt-1")).thenReturn(true);
        assertThat(deduplicator.isProcessed("evt-1")).isTrue();

        when(repository.existsById("evt-2")).thenReturn(false);
        assertThat(deduplicator.isProcessed("evt-2")).isFalse();
    }
}
