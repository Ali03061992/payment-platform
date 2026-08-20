package com.paymentplatform.identity.infrastructure.messaging;

import com.paymentplatform.shared.infrastructure.outbox.AmqpTopology;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Déclaration de la file identity.organization-status sur l'exchange organization.events. */
@Configuration
public class IdentityAmqpConfig {

    @Bean
    public TopicExchange organizationExchange() {
        return new TopicExchange(AmqpTopology.EXCHANGE_ORGANIZATION, true, false);
    }

    @Bean
    public Queue identityOrganizationStatusQueue() {
        return new Queue(AmqpTopology.QUEUE_IDENTITY_ORG_STATUS, true);
    }

    @Bean
    public Binding identityOrganizationStatusBinding(Queue identityOrganizationStatusQueue,
                                                     TopicExchange organizationExchange) {
        return BindingBuilder.bind(identityOrganizationStatusQueue)
                .to(organizationExchange)
                .with("organization.supplier.disabled");
    }

    @Bean
    public Binding identityOrganizationStatusShopBinding(Queue identityOrganizationStatusQueue,
                                                         TopicExchange organizationExchange) {
        return BindingBuilder.bind(identityOrganizationStatusQueue)
                .to(organizationExchange)
                .with("organization.shop.disabled");
    }
}