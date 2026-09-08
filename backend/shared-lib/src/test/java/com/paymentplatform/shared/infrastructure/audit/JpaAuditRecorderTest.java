package com.paymentplatform.shared.infrastructure.audit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class JpaAuditRecorderTest {

    @Autowired
    private JpaAuditRecorder recorder;

    @Autowired
    private AuditLogRepository repository;

    @Test
    void record_savesAuditEntry() {
        recorder.record(1L, 10L, "PAYMENT_CREATED", 100L, "Test details");

        assertThat(repository.count()).isEqualTo(1);
        AuditLogEntity saved = repository.findAll().get(0);
        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(saved.getOrganizationId()).isEqualTo(10L);
        assertThat(saved.getAction()).isEqualTo("PAYMENT_CREATED");
        assertThat(saved.getEntityId()).isEqualTo(100L);
        assertThat(saved.getDetails()).isEqualTo("Test details");
        assertThat(saved.getTimestamp()).isNotNull();
    }

    @Test
    void record_withNullDetails() {
        recorder.record(2L, null, "USER_LOGIN", null, null);

        AuditLogEntity saved = repository.findAll().get(0);
        assertThat(saved.getUserId()).isEqualTo(2L);
        assertThat(saved.getOrganizationId()).isNull();
        assertThat(saved.getDetails()).isNull();
    }

    @Test
    void record_multipleEntries() {
        recorder.record(1L, null, "ACTION_1", null, null);
        recorder.record(2L, null, "ACTION_2", null, null);
        recorder.record(3L, null, "ACTION_3", null, null);
        assertThat(repository.count()).isEqualTo(3);
    }
}
