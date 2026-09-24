package com.paymentplatform.organization.infrastructure.messaging;

import com.paymentplatform.organization.application.usecase.CancelExpiredOrdersUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CancelExpiredOrdersSchedulerTest {

    @Autowired
    private CancelExpiredOrdersScheduler scheduler;

    @MockitoBean
    private CancelExpiredOrdersUseCase cancelExpiredOrdersUseCase;

    @Test
    void cancelExpiredOrders_delegatesToUseCase() {
        scheduler.cancelExpiredOrders();
        verify(cancelExpiredOrdersUseCase).execute();
    }
}
