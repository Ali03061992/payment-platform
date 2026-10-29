package com.paymentplatform.notification.application;

import com.paymentplatform.notification.domain.model.WebPushSubscription;
import com.paymentplatform.notification.domain.model.WebPushSubscriptionRepository;
import com.paymentplatform.shared.domain.exception.ConflictException;
import com.paymentplatform.shared.domain.exception.DomainException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Inscription/désinscription des abonnements Web Push (PWA). */
@Service
public class WebPushSubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(WebPushSubscriptionService.class);

    private final WebPushSubscriptionRepository repository;
    private final WebPushEndpointPolicy endpointPolicy;
    private final int maxSubscriptionsPerUser;

    public WebPushSubscriptionService(WebPushSubscriptionRepository repository,
                                      WebPushEndpointPolicy endpointPolicy,
                                      @Value("${app.webpush.max-subscriptions-per-user:10}")
                                      int maxSubscriptionsPerUser) {
        this.repository = repository;
        this.endpointPolicy = endpointPolicy;
        this.maxSubscriptionsPerUser = maxSubscriptionsPerUser;
    }

    @Transactional
    public WebPushSubscription register(UUID userId, UUID organizationId, String endpoint,
                                        String p256dh, String auth, String userAgent) {
        if (!endpointPolicy.isAllowed(endpoint)) {
            throw new DomainException("Endpoint Web Push invalide ou non autorise");
        }
        var existing = repository.findByEndpoint(endpoint);
        WebPushSubscription subscription;
        if (existing.isPresent()) {
            subscription = existing.get();
            if (!subscription.userId().equals(userId)) {
                log.warn("Web Push reassignment rejected for user {} (endpoint owned by another account)", userId);
                throw new ConflictException("Cet abonnement Web Push est deja associe a un autre compte");
            }
            subscription.refreshCredentials(organizationId, p256dh, auth, userAgent);
        } else {
            if (repository.findByUserId(userId).size() >= maxSubscriptionsPerUser) {
                throw new ConflictException("Nombre maximal d'abonnements Web Push atteint");
            }
            subscription = new WebPushSubscription(userId, organizationId, endpoint, p256dh, auth, userAgent);
        }
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

    @Transactional(readOnly = true)
    public List<WebPushSubscription> listByOrganization(UUID organizationId) {
        return repository.findByOrganizationId(organizationId);
    }

    @Transactional
    public void removeByEndpoint(String endpoint) {
        repository.deleteByEndpoint(endpoint);
    }
}
