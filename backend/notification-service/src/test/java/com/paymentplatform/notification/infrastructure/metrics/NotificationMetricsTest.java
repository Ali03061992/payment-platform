package com.paymentplatform.notification.infrastructure.metrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationMetricsTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final NotificationMetrics metrics = new NotificationMetrics(registry);

    @Test
    void notificationCreated_isTaggedByType() {
        metrics.notificationCreated("DISPUTE_CREATED");
        metrics.notificationCreated("DISPUTE_CREATED");

        assertThat(registry.get("notification.created")
                .tag("type", "DISPUTE_CREATED")
                .counter().count()).isEqualTo(2.0);
    }

    @Test
    void notificationCreated_handlesNullType() {
        metrics.notificationCreated(null);

        assertThat(registry.get("notification.created")
                .tag("type", "unknown")
                .counter().count()).isEqualTo(1.0);
    }

    @Test
    void sseCounters_areIncremented() {
        metrics.sseDelivered();
        metrics.sseDelivered();
        metrics.sseDropped();

        assertThat(registry.get("notification.sse.delivered").counter().count()).isEqualTo(2.0);
        assertThat(registry.get("notification.sse.dropped").counter().count()).isEqualTo(1.0);
    }

    @Test
    void sseGauge_reflectsActiveConnections() {
        int[] active = {0};
        metrics.registerSseGauge(() -> active[0]);

        assertThat(registry.get("notification.sse.active").gauge().value()).isZero();

        active[0] = 4;

        assertThat(registry.get("notification.sse.active").gauge().value()).isEqualTo(4.0);
    }

    @Test
    void pushCounters_areIncremented() {
        metrics.fcmSent();
        metrics.fcmFailed();
        metrics.webPushSent();
        metrics.webPushFailed();
        metrics.webPushSubscriptionRemoved();

        assertThat(registry.get("notification.push.fcm.sent").counter().count()).isEqualTo(1.0);
        assertThat(registry.get("notification.push.fcm.failed").counter().count()).isEqualTo(1.0);
        assertThat(registry.get("notification.push.web.sent").counter().count()).isEqualTo(1.0);
        assertThat(registry.get("notification.push.web.failed").counter().count()).isEqualTo(1.0);
        assertThat(registry.get("notification.push.web.removed").counter().count()).isEqualTo(1.0);
    }
}
