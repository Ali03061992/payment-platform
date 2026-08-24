package com.paymentplatform.organization.infrastructure.messaging;

import com.paymentplatform.shared.infrastructure.outbox.AmqpTopology;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrganizationAmqpConfig {

    @Bean
    public TopicExchange organizationExchange() {
        return new TopicExchange(AmqpTopology.EXCHANGE_ORGANIZATION);
    }

    @Bean
    public Queue organizationEventsQueue() {
        return new Queue("organization.events", true);
    }

    @Bean
    public Binding organizationEventsBinding(TopicExchange organizationExchange) {
        return BindingBuilder.bind(organizationEventsQueue())
                .to(organizationExchange)
                .with("organization.#");
    }
}
