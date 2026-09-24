package com.paymentplatform.notification.infrastructure.messaging;

import com.paymentplatform.shared.infrastructure.outbox.AmqpTopology;
import org.springframework.amqp.core.*;
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
        return QueueBuilder.durable(AmqpTopology.QUEUE_NOTIFICATION_PAYMENTS)
                .deadLetterExchange(AmqpTopology.EXCHANGE_PAYMENT)
                .deadLetterRoutingKey(AmqpTopology.QUEUE_NOTIFICATION_PAYMENTS + ".DLQ")
                .build();
    }

    @Bean
    public Queue notificationPaymentsDlq() {
        return new Queue(AmqpTopology.QUEUE_NOTIFICATION_PAYMENTS + ".DLQ", true);
    }

    @Bean
    public Queue notificationOrdersQueue() {
        return QueueBuilder.durable(AmqpTopology.QUEUE_NOTIFICATION_ORDERS)
                .deadLetterExchange(AmqpTopology.EXCHANGE_ORGANIZATION)
                .deadLetterRoutingKey(AmqpTopology.QUEUE_NOTIFICATION_ORDERS + ".DLQ")
                .build();
    }

    @Bean
    public Queue notificationOrdersDlq() {
        return new Queue(AmqpTopology.QUEUE_NOTIFICATION_ORDERS + ".DLQ", true);
    }

    @Bean
    public Queue notificationOrganizationsQueue() {
        return new Queue(AmqpTopology.QUEUE_NOTIFICATION_ORGANIZATIONS, true);
    }

    @Bean
    public Queue notificationUsersQueue() {
        return QueueBuilder.durable(AmqpTopology.QUEUE_NOTIFICATION_USERS).build();
    }

    @Bean
    public Queue notificationDisputesQueue() {
        return QueueBuilder.durable(AmqpTopology.QUEUE_NOTIFICATION_DISPUTES)
                .deadLetterExchange(AmqpTopology.EXCHANGE_ORGANIZATION)
                .deadLetterRoutingKey(AmqpTopology.QUEUE_NOTIFICATION_DISPUTES + ".DLQ")
                .build();
    }

    @Bean
    public Queue notificationDisputesDlq() {
        return new Queue(AmqpTopology.QUEUE_NOTIFICATION_DISPUTES + ".DLQ", true);
    }

    @Bean
    public Binding notificationPaymentsBinding(Queue notificationPaymentsQueue, TopicExchange paymentExchange) {
        return BindingBuilder.bind(notificationPaymentsQueue)
                .to(paymentExchange)
                .with("payment.*");
    }

    @Bean
    public Binding notificationPaymentsDlqBinding(Queue notificationPaymentsDlq, TopicExchange paymentExchange) {
        return BindingBuilder.bind(notificationPaymentsDlq)
                .to(paymentExchange)
                .with(AmqpTopology.QUEUE_NOTIFICATION_PAYMENTS + ".DLQ");
    }

    @Bean
    public Binding notificationOrdersBinding(Queue notificationOrdersQueue, TopicExchange organizationExchange) {
        return BindingBuilder.bind(notificationOrdersQueue)
                .to(organizationExchange)
                .with("order.*");
    }

    @Bean
    public Binding notificationOrdersDlqBinding(Queue notificationOrdersDlq, TopicExchange organizationExchange) {
        return BindingBuilder.bind(notificationOrdersDlq)
                .to(organizationExchange)
                .with(AmqpTopology.QUEUE_NOTIFICATION_ORDERS + ".DLQ");
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

    @Bean
    public Binding notificationDisputesBinding(Queue notificationDisputesQueue, TopicExchange organizationExchange) {
        return BindingBuilder.bind(notificationDisputesQueue)
                .to(organizationExchange)
                .with("dispute.*");
    }

    @Bean
    public Binding notificationDisputesDlqBinding(Queue notificationDisputesDlq, TopicExchange organizationExchange) {
        return BindingBuilder.bind(notificationDisputesDlq)
                .to(organizationExchange)
                .with(AmqpTopology.QUEUE_NOTIFICATION_DISPUTES + ".DLQ");
    }
}
