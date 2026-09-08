package com.paymentplatform.identity.infrastructure.messaging;

import com.paymentplatform.shared.infrastructure.outbox.AmqpTopology;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class IdentityAmqpConfigTest {

    @Autowired private TopicExchange organizationExchange;
    @Autowired private Queue identityOrganizationStatusQueue;
    @Autowired private Binding identityOrganizationStatusBinding;
    @Autowired private Binding identityOrganizationStatusShopBinding;

    @Test
    void organizationExchange_isCorrect() {
        assertThat(organizationExchange.getName()).isEqualTo(AmqpTopology.EXCHANGE_ORGANIZATION);
    }

    @Test
    void identityOrganizationStatusQueue_isCorrect() {
        assertThat(identityOrganizationStatusQueue.getName()).isEqualTo(AmqpTopology.QUEUE_IDENTITY_ORG_STATUS);
        assertThat(identityOrganizationStatusQueue.isDurable()).isTrue();
    }

    @Test
    void identityOrganizationStatusBinding_bindsCorrectly() {
        assertThat(identityOrganizationStatusBinding.getExchange()).isEqualTo(AmqpTopology.EXCHANGE_ORGANIZATION);
        assertThat(identityOrganizationStatusBinding.getRoutingKey()).isEqualTo("organization.supplier.disabled");
    }

    @Test
    void identityOrganizationStatusShopBinding_bindsCorrectly() {
        assertThat(identityOrganizationStatusShopBinding.getExchange()).isEqualTo(AmqpTopology.EXCHANGE_ORGANIZATION);
        assertThat(identityOrganizationStatusShopBinding.getRoutingKey()).isEqualTo("organization.shop.disabled");
    }
}
