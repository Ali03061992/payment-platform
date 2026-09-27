package com.paymentplatform.organization.infrastructure.messaging;

import com.paymentplatform.organization.application.usecase.AutoAcceptDeliveriesUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.Mockito.verify;
/**
 * Tests de AutoAcceptDeliveriesSchedulerTest.
 * Perimetre : infrastructure AutoAcceptDeliveriesScheduler (messagerie/config/persistance).
 * Moyens : contexte SpringBootTest, profil "test" (H2), mocks Mockito.
 */

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AutoAcceptDeliveriesSchedulerTest {

    @Autowired
    private AutoAcceptDeliveriesScheduler scheduler;

    @MockitoBean
    private AutoAcceptDeliveriesUseCase autoAcceptDeliveriesUseCase;

    @Test
    void autoAcceptDeliveries_delegatesToUseCase() {
        scheduler.autoAcceptDeliveries();
        verify(autoAcceptDeliveriesUseCase).execute();
    }
}
