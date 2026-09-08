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
class AuditLogRepositoryTest {

    @Autowired
    private AuditLogRepository repository;

    @Test
    void save_andFindById() {
        AuditLogEntity entity = new AuditLogEntity(1L, 10L, "PAYMENT_CREATED", 100L, "Payment created");
        AuditLogEntity saved = repository.save(entity);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getAction()).isEqualTo("PAYMENT_CREATED");
        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(saved.getOrganizationId()).isEqualTo(10L);
        assertThat(saved.getEntityId()).isEqualTo(100L);
        assertThat(saved.getDetails()).isEqualTo("Payment created");
        assertThat(saved.getTimestamp()).isNotNull();
    }

    @Test
    void save_multipleAndCount() {
        repository.save(new AuditLogEntity(1L, null, "ACTION_A", null, null));
        repository.save(new AuditLogEntity(2L, 10L, "ACTION_B", 50L, "details"));
        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    void findAll_returnsAll() {
        repository.save(new AuditLogEntity(1L, null, "ACTION_A", null, null));
        repository.save(new AuditLogEntity(2L, null, "ACTION_B", null, null));
        assertThat(repository.findAll()).hasSize(2);
    }

    @Test
    void deleteAll_clearsRepository() {
        repository.save(new AuditLogEntity(1L, null, "ACTION_A", null, null));
        repository.deleteAll();
        assertThat(repository.count()).isEqualTo(0);
    }
}
