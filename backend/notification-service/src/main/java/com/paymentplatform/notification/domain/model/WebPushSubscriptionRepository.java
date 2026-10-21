package com.paymentplatform.notification.domain.model;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WebPushSubscriptionRepository extends JpaRepository<WebPushSubscription, UUID> {

    List<WebPushSubscription> findByUserId(UUID userId);

    List<WebPushSubscription> findByOrganizationId(UUID organizationId);

    Optional<WebPushSubscription> findByEndpoint(String endpoint);

    void deleteByEndpoint(String endpoint);

    void deleteByUserId(UUID userId);
}
