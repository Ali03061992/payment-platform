package com.paymentplatform.shared.infrastructure.outbox;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
/**
 * Tests de AmqpTopologyTest.
 * Perimetre : infrastructure AmqpTopology (messagerie/config/persistance).
 * Moyens : AMQP (mocks/config).
 */

class AmqpTopologyTest {

    private final AmqpTopology topology = new AmqpTopology();

    @Test
    void exchangeFor_paymentEvent_returnsPaymentExchange() {
        assertThat(topology.exchangeFor("payment.created")).isEqualTo(AmqpTopology.EXCHANGE_PAYMENT);
        assertThat(topology.exchangeFor("payment.confirmed")).isEqualTo(AmqpTopology.EXCHANGE_PAYMENT);
    }

    @Test
    void exchangeFor_orderEvent_returnsOrganizationExchange() {
        assertThat(topology.exchangeFor("order.created")).isEqualTo(AmqpTopology.EXCHANGE_ORGANIZATION);
        assertThat(topology.exchangeFor("order.confirmed")).isEqualTo(AmqpTopology.EXCHANGE_ORGANIZATION);
    }

    @Test
    void exchangeFor_organizationEvent_returnsOrganizationExchange() {
        assertThat(topology.exchangeFor("organization.supplier.disabled")).isEqualTo(AmqpTopology.EXCHANGE_ORGANIZATION);
        assertThat(topology.exchangeFor("organization.shop.disabled")).isEqualTo(AmqpTopology.EXCHANGE_ORGANIZATION);
    }

    @Test
    void exchangeFor_identityEvent_returnsIdentityExchange() {
        assertThat(topology.exchangeFor("identity.user.created")).isEqualTo(AmqpTopology.EXCHANGE_IDENTITY);
    }

    @Test
    void exchangeFor_unknownType_throwsIllegalArgument() {
        assertThatThrownBy(() -> topology.exchangeFor("unknown.event"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("inconnu");
    }

    @Test
    void constants_areCorrect() {
        assertThat(AmqpTopology.EXCHANGE_PAYMENT).isEqualTo("payment.events");
        assertThat(AmqpTopology.EXCHANGE_ORGANIZATION).isEqualTo("organization.events");
        assertThat(AmqpTopology.EXCHANGE_IDENTITY).isEqualTo("identity.events");
        assertThat(AmqpTopology.QUEUE_NOTIFICATION_PAYMENTS).isEqualTo("notification.payments");
        assertThat(AmqpTopology.QUEUE_NOTIFICATION_ORDERS).isEqualTo("notification.orders");
        assertThat(AmqpTopology.QUEUE_IDENTITY_ORG_STATUS).isEqualTo("identity.organization-status");
    }
}
