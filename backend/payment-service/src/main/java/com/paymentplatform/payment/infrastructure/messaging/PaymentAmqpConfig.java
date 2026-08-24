package com.paymentplatform.payment.infrastructure.messaging;

import com.paymentplatform.shared.infrastructure.outbox.AmqpTopology;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentAmqpConfig {

    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(AmqpTopology.EXCHANGE_PAYMENT);
    }

    @Bean
    public Queue paymentEventsQueue() {
        return new Queue("payment.events", true);
    }

    @Bean
    public Binding paymentEventsBinding(TopicExchange paymentExchange) {
        return BindingBuilder.bind(paymentEventsQueue())
                .to(paymentExchange)
                .with("payment.#");
    }
}
