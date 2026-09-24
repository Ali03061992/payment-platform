package com.paymentplatform.organization.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentplatform.organization.application.usecase.BalanceUseCase;
import com.paymentplatform.shared.infrastructure.eventing.EventDeduplicator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class PaymentConfirmedConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentConfirmedConsumer.class);

    private final BalanceUseCase balanceUseCase;
    private final EventDeduplicator deduplicator;
    private final ObjectMapper objectMapper;

    public PaymentConfirmedConsumer(BalanceUseCase balanceUseCase,
                                    EventDeduplicator deduplicator,
                                    ObjectMapper objectMapper) {
        this.balanceUseCase = balanceUseCase;
        this.deduplicator = deduplicator;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "organization.payments")
    public void onPaymentConfirmed(Message message) {
        try {
            JsonNode event = objectMapper.readTree(message.getBody());
            String eventId = event.get("eventId").asText();
            String routingKey = message.getMessageProperties().getReceivedRoutingKey();

            if (!"payment.confirmed".equals(routingKey)) {
                return;
            }
            if (!deduplicator.markProcessed(eventId)) {
                log.debug("Event {} already processed, skipping", eventId);
                return;
            }

            UUID paymentId = UUID.fromString(event.get("paymentId").asText());
            UUID shopId = UUID.fromString(event.get("shopId").asText());
            UUID supplierId = UUID.fromString(event.get("supplierId").asText());
            UUID confirmedBy = UUID.fromString(event.get("confirmedBy").asText());
            BigDecimal amount = new BigDecimal(event.get("amount").asText());

            balanceUseCase.debitBalance(supplierId, shopId, amount, paymentId, confirmedBy);
            log.info("Balance debited for payment {} amount {}", paymentId, amount);
        } catch (Exception e) {
            log.error("Failed to process payment.confirmed for balance debit", e);
        }
    }
}
