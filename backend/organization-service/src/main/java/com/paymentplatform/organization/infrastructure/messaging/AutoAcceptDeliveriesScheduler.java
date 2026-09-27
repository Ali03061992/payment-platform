package com.paymentplatform.organization.infrastructure.messaging;

import com.paymentplatform.organization.application.usecase.AutoAcceptDeliveriesUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AutoAcceptDeliveriesScheduler {

    private static final Logger log = LoggerFactory.getLogger(AutoAcceptDeliveriesScheduler.class);

    private final AutoAcceptDeliveriesUseCase autoAcceptDeliveriesUseCase;

    public AutoAcceptDeliveriesScheduler(AutoAcceptDeliveriesUseCase autoAcceptDeliveriesUseCase) {
        this.autoAcceptDeliveriesUseCase = autoAcceptDeliveriesUseCase;
    }

    @Scheduled(cron = "0 */5 * * * ?")
    public void autoAcceptDeliveries() {
        log.info("Starting scheduled auto-accept of stale deliveries");
        try {
            int accepted = autoAcceptDeliveriesUseCase.execute();
            log.info("Finished scheduled auto-accept of stale deliveries: {} accepted", accepted);
        } catch (Exception e) {
            log.error("Error during scheduled auto-accept of stale deliveries", e);
        }
    }
}
