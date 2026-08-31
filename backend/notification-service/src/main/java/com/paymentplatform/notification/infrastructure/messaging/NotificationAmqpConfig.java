package com.paymentplatform.notification.infrastructure.messaging;

import com.paymentplatform.shared.infrastructure.outbox.AmqpTopology;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NotificationAmqpConfig {

    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(AmqpTopology.EXCHANGE_PAYMENT);
    }

    @Bean
    public TopicExchange organizationExchange() {
        return new TopicExchange(AmqpTopology.EXCHANGE_ORGANIZATION);
    }

    @Bean
    public TopicExchange identityExchange() {
        return new TopicExchange(AmqpTopology.EXCHANGE_IDENTITY);
    }

    @Bean
    public Queue notificationPaymentsQueue() {
        return new Queue(AmqpTopology.QUEUE_NOTIFICATION_PAYMENTS, true);
    }

    @Bean
    public Queue notificationOrganizationsQueue() {
        return new Queue(AmqpTopology.QUEUE_NOTIFICATION_ORGANIZATIONS, true);
    }

    @Bean
    public Queue notificationUsersQueue() {
        return new Queue(AmqpTopology.QUEUE_NOTIFICATION_USERS, true);
    }

    @Bean
    public Binding notificationPaymentsBinding(Queue notificationPaymentsQueue, TopicExchange paymentExchange) {
        return BindingBuilder.bind(notificationPaymentsQueue)
                .to(paymentExchange)
                .with("payment.*");
    }

    @Bean
    public Binding notificationOrganizationsBinding(Queue notificationOrganizationsQueue, TopicExchange organizationExchange) {
        return BindingBuilder.bind(notificationOrganizationsQueue)
                .to(organizationExchange)
                .with("organization.*");
    }

    @Bean
    public Binding notificationUsersBinding(Queue notificationUsersQueue, TopicExchange identityExchange) {
        return BindingBuilder.bind(notificationUsersQueue)
                .to(identityExchange)
                .with("identity.*");
    }
}
