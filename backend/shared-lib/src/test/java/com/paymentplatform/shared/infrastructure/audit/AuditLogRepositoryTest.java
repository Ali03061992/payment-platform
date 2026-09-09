package com.paymentplatform.shared.infrastructure.audit;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuditLogRepositoryTest {

    @Autowired
    private AuditLogRepository repository;

    @Test
    void save_andFindById() {
        AuditLogEntity entity = new AuditLogEntity(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000010"), "PAYMENT_CREATED", UUID.fromString("00000000-0000-0000-0000-000000000100"), "Payment created");
        AuditLogEntity saved = repository.save(entity);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getAction()).isEqualTo("PAYMENT_CREATED");
        assertThat(saved.getUserId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        assertThat(saved.getOrganizationId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000010"));
        assertThat(saved.getEntityId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000100"));
        assertThat(saved.getDetails()).isEqualTo("Payment created");
        assertThat(saved.getTimestamp()).isNotNull();
    }

    @Test
    void save_multipleAndCount() {
        repository.save(new AuditLogEntity(UUID.fromString("00000000-0000-0000-0000-000000000001"), null, "ACTION_A", null, null));
        repository.save(new AuditLogEntity(UUID.fromString("00000000-0000-0000-0000-000000000002"), UUID.fromString("00000000-0000-0000-0000-000000000010"), "ACTION_B", UUID.fromString("00000000-0000-0000-0000-000000000050"), "details"));
        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    void findAll_returnsAll() {
        repository.save(new AuditLogEntity(UUID.fromString("00000000-0000-0000-0000-000000000001"), null, "ACTION_A", null, null));
        repository.save(new AuditLogEntity(UUID.fromString("00000000-0000-0000-0000-000000000002"), null, "ACTION_B", null, null));
        assertThat(repository.findAll()).hasSize(2);
    }

    @Test
    void deleteAll_clearsRepository() {
        repository.save(new AuditLogEntity(UUID.fromString("00000000-0000-0000-0000-000000000001"), null, "ACTION_A", null, null));
        repository.deleteAll();
        assertThat(repository.count()).isEqualTo(0);
    }
}
