package com.paymentplatform.notification.application;

import com.paymentplatform.notification.domain.model.WebPushSubscription;
import com.paymentplatform.notification.domain.model.WebPushSubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Inscription/désinscription des abonnements Web Push (PWA). */
@Service
public class WebPushSubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(WebPushSubscriptionService.class);

    private final WebPushSubscriptionRepository repository;

    public WebPushSubscriptionService(WebPushSubscriptionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public WebPushSubscription register(UUID userId, UUID organizationId, String endpoint,
                                        String p256dh, String auth, String userAgent) {
        var existing = repository.findByEndpoint(endpoint);
        WebPushSubscription subscription = existing
                .orElseGet(() -> new WebPushSubscription(userId, organizationId, endpoint, p256dh, auth, userAgent));
        subscription.touch();
        WebPushSubscription saved = repository.save(subscription);
        log.info("Web Push subscription registered for user {} (endpoint {}...)",
                userId, endpoint.substring(0, Math.min(40, endpoint.length())));
        return saved;
    }

    @Transactional
    public void unregister(UUID userId, String endpoint) {
        repository.findByEndpoint(endpoint)
                .filter(sub -> sub.userId().equals(userId))
                .ifPresent(sub -> {
                    repository.delete(sub);
                    log.info("Web Push subscription removed for user {}", userId);
                });
    }

    @Transactional
    public void unregisterById(UUID userId, UUID subscriptionId) {
        repository.findById(subscriptionId)
                .filter(sub -> sub.userId().equals(userId))
                .ifPresent(repository::delete);
    }

    @Transactional(readOnly = true)
    public List<WebPushSubscription> list(UUID userId) {
        return repository.findByUserId(userId);
    }

    @Transactional
    public void removeByEndpoint(String endpoint) {
        repository.deleteByEndpoint(endpoint);
    }
}
