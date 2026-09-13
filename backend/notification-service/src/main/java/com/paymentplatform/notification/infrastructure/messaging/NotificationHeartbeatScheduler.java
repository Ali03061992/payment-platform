package com.paymentplatform.notification.infrastructure.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NotificationHeartbeatScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotificationHeartbeatScheduler.class);

    private final NotificationBroadcaster broadcaster;

    public NotificationHeartbeatScheduler(NotificationBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    @Scheduled(fixedDelay = 30000)
    public void sendHeartbeat() {
        int count = broadcaster.getActiveCount();
        if (count > 0) {
            broadcaster.sendHeartbeat();
            log.debug("Heartbeat sent to {} SSE connections", count);
        }
    }
}
