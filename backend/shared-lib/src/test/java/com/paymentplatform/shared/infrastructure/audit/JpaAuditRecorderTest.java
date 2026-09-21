package com.paymentplatform.shared.infrastructure.audit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

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
        recorder.record(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000010"), "PAYMENT_CREATED", UUID.fromString("00000000-0000-0000-0000-000000000100"), "Test details");

        assertThat(repository.count()).isEqualTo(1);
        AuditLogEntity saved = repository.findAll().get(0);
        assertThat(saved.getUserId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        assertThat(saved.getOrganizationId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000010"));
        assertThat(saved.getAction()).isEqualTo("PAYMENT_CREATED");
        assertThat(saved.getEntityId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000100"));
        assertThat(saved.getDetails()).isEqualTo("Test details");
        assertThat(saved.getTimestamp()).isNotNull();
    }

    @Test
    void record_withNullDetails() {
        recorder.record(UUID.fromString("00000000-0000-0000-0000-000000000002"), null, "USER_LOGIN", null, null);

        AuditLogEntity saved = repository.findAll().get(0);
        assertThat(saved.getUserId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        assertThat(saved.getOrganizationId()).isNull();
        assertThat(saved.getDetails()).isNull();
    }

    @Test
    void record_multipleEntries() {
        recorder.record(UUID.fromString("00000000-0000-0000-0000-000000000001"), null, "ACTION_1", null, null);
        recorder.record(UUID.fromString("00000000-0000-0000-0000-000000000002"), null, "ACTION_2", null, null);
        recorder.record(UUID.fromString("00000000-0000-0000-0000-000000000003"), null, "ACTION_3", null, null);
        assertThat(repository.count()).isEqualTo(3);
    }
}
