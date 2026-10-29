package com.paymentplatform.notification.application;

import com.paymentplatform.notification.domain.model.WebPushSubscription;
import com.paymentplatform.notification.domain.model.WebPushSubscriptionRepository;
import com.paymentplatform.shared.domain.exception.ConflictException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "app.webpush.max-subscriptions-per-user=2")
@ActiveProfiles("test")
@Transactional
class WebPushSubscriptionServiceTest {

    private static final UUID USER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_USER = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID ORGANIZATION = UUID.fromString("00000000-0000-0000-0000-000000000010");

    @Autowired
    private WebPushSubscriptionService service;

    @Autowired
    private WebPushSubscriptionRepository repository;

    @Test
    void register_sameUser_refreshesWithoutDuplicating() {
        service.register(USER, ORGANIZATION, "https://fcm.googleapis.com/one", "key-1", "auth-1", "agent");
        service.register(USER, ORGANIZATION, "https://fcm.googleapis.com/one", "key-2", "auth-2", "agent-2");

        assertThat(repository.findByUserId(USER)).hasSize(1);
        WebPushSubscription subscription = repository.findByEndpoint("https://fcm.googleapis.com/one").orElseThrow();
        assertThat(subscription.p256dh()).isEqualTo("key-2");
        assertThat(subscription.auth()).isEqualTo("auth-2");
    }

    @Test
    void register_existingEndpointOwnedByAnotherUser_throwsConflict() {
        service.register(USER, ORGANIZATION, "https://fcm.googleapis.com/shared", "key", "auth", "agent");

        assertThatThrownBy(() -> service.register(
                OTHER_USER, ORGANIZATION, "https://fcm.googleapis.com/shared", "key", "auth", "agent"))
                .isInstanceOf(ConflictException.class);
        assertThat(repository.findByEndpoint("https://fcm.googleapis.com/shared").orElseThrow().userId())
                .isEqualTo(USER);
    }

    @Test
    void register_overSubscriptionLimit_throwsConflict() {
        service.register(USER, ORGANIZATION, "https://fcm.googleapis.com/one", "key", "auth", "agent");
        service.register(USER, ORGANIZATION, "https://fcm.googleapis.com/two", "key", "auth", "agent");

        assertThatThrownBy(() -> service.register(
                USER, ORGANIZATION, "https://fcm.googleapis.com/three", "key", "auth", "agent"))
                .isInstanceOf(ConflictException.class);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class AllowAllPolicy {

        @Bean
        @Primary
        WebPushEndpointPolicy allowAllEndpoints() {
            return endpoint -> true;
        }
    }
}
