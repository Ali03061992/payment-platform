package com.paymentplatform.shared.infrastructure.outbox;

import org.springframework.stereotype.Component;

/** Topologie RabbitMQ : correspondance type d'événement → exchange. */
@Component
public class AmqpTopology {

    public static final String EXCHANGE_PAYMENT = "payment.events";
    public static final String EXCHANGE_ORGANIZATION = "organization.events";
    public static final String EXCHANGE_IDENTITY = "identity.events";

    public static final String QUEUE_NOTIFICATION_PAYMENTS = "notification.payments";
    public static final String QUEUE_NOTIFICATION_ORDERS = "notification.orders";
    public static final String QUEUE_NOTIFICATION_ORGANIZATIONS = "notification.organizations";
    public static final String QUEUE_NOTIFICATION_USERS = "notification.users";
    public static final String QUEUE_IDENTITY_ORG_STATUS = "identity.organization-status";

    public String exchangeFor(String eventType) {
        if (eventType.startsWith("payment.")) {
            return EXCHANGE_PAYMENT;
        }
        if (eventType.startsWith("order.")) {
            return EXCHANGE_ORGANIZATION;
        }
        if (eventType.startsWith("organization.")) {
            return EXCHANGE_ORGANIZATION;
        }
        if (eventType.startsWith("identity.")) {
            return EXCHANGE_IDENTITY;
        }
        throw new IllegalArgumentException("Type d'événement inconnu : " + eventType);
    }
}