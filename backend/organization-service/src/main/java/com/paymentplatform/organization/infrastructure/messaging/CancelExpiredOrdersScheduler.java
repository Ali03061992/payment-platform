package com.paymentplatform.organization.infrastructure.messaging;

import com.paymentplatform.organization.application.usecase.CancelExpiredOrdersUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CancelExpiredOrdersScheduler {

    private static final Logger log = LoggerFactory.getLogger(CancelExpiredOrdersScheduler.class);

    private final CancelExpiredOrdersUseCase cancelExpiredOrdersUseCase;

    public CancelExpiredOrdersScheduler(CancelExpiredOrdersUseCase cancelExpiredOrdersUseCase) {
        this.cancelExpiredOrdersUseCase = cancelExpiredOrdersUseCase;
    }

    @Scheduled(cron = "0 0 2 * * ?")
    public void cancelExpiredOrders() {
        log.info("Starting scheduled cancellation of expired orders");
        try {
            cancelExpiredOrdersUseCase.execute();
            log.info("Finished scheduled cancellation of expired orders");
        } catch (Exception e) {
            log.error("Error during scheduled cancellation of expired orders", e);
        }
    }
}
