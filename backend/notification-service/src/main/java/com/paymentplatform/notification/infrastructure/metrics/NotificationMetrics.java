package com.paymentplatform.notification.infrastructure.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * Compteurs Micrometer du service notification : notifications creees,
 * livraisons SSE, envois FCM et Web Push (succes / echecs / nettoyages).
 */
@Component
public class NotificationMetrics {

    private final MeterRegistry registry;

    public NotificationMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void notificationCreated(String type) {
        registry.counter("notification.created", "type", type == null ? "unknown" : type).increment();
    }

    public void sseDelivered() {
        registry.counter("notification.sse.delivered").increment();
    }

    public void sseDropped() {
        registry.counter("notification.sse.dropped").increment();
    }

    public void registerSseGauge(Supplier<Number> activeConnections) {
        Gauge.builder("notification.sse.active", activeConnections)
                .strongReference(true)
                .register(registry);
    }

    public void fcmSent() {
        registry.counter("notification.push.fcm.sent").increment();
    }

    public void fcmFailed() {
        registry.counter("notification.push.fcm.failed").increment();
    }

    public void webPushSent() {
        registry.counter("notification.push.web.sent").increment();
    }

    public void webPushFailed() {
        registry.counter("notification.push.web.failed").increment();
    }

    public void webPushSubscriptionRemoved() {
        registry.counter("notification.push.web.removed").increment();
    }
}
