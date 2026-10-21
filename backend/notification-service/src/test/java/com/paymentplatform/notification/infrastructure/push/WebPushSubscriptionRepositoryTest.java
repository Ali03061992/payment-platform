package com.paymentplatform.notification.infrastructure.push;

import com.paymentplatform.notification.application.WebPushSubscriptionService;
import com.paymentplatform.notification.domain.model.WebPushSubscription;
import com.paymentplatform.notification.domain.model.WebPushSubscriptionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de WebPushSubscriptionRepositoryTest.
 * Perimetre : selection des abonnements Web Push par organisation.
 * Moyens : contexte SpringBootTest, profil "test" (H2), transaction rollback.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WebPushSubscriptionRepositoryTest {

    @Autowired
    private WebPushSubscriptionRepository repository;

    @Autowired
    private WebPushSubscriptionService service;

    @Test
    void listByOrganization_returnsOnlySubscriptionsOfThatOrganization() {
        UUID organizationA = UUID.randomUUID();
        UUID organizationB = UUID.randomUUID();
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();

        repository.save(new WebPushSubscription(userA, organizationA,
                "https://push.example.com/a", "p256dh-a", "auth-a", "agent-a"));
        repository.save(new WebPushSubscription(userB, organizationB,
                "https://push.example.com/b", "p256dh-b", "auth-b", "agent-b"));

        assertThat(service.listByOrganization(organizationA))
                .extracting(WebPushSubscription::endpoint)
                .containsExactly("https://push.example.com/a");
        assertThat(service.listByOrganization(organizationB))
                .extracting(WebPushSubscription::endpoint)
                .containsExactly("https://push.example.com/b");
    }

    @Test
    void listByUser_doesNotMatchOrganizationId() {
        UUID organizationId = UUID.randomUUID();
        repository.save(new WebPushSubscription(UUID.randomUUID(), organizationId,
                "https://push.example.com/organization", "p256dh", "auth", "agent"));

        assertThat(service.list(organizationId)).isEmpty();
        assertThat(service.listByOrganization(organizationId)).hasSize(1);
    }
}
