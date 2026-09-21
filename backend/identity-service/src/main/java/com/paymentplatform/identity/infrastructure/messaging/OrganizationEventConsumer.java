package com.paymentplatform.identity.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentplatform.identity.application.usecase.OrganizationCascadeUseCase;
import com.paymentplatform.shared.domain.event.OrganizationEvents.ShopDisabledEvent;
import com.paymentplatform.shared.domain.event.OrganizationEvents.SupplierDisabledEvent;
import com.paymentplatform.shared.domain.model.RoleCode;
import com.paymentplatform.shared.infrastructure.outbox.AmqpTopology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Consommateur des événements de désactivation d'organisations.
 * SupplierDisabledEvent ⇒ désactive SUPPLIER_ADMIN + SUPPLIER_AGENT.
 * ShopDisabledEvent ⇒ désactive SHOP_ADMIN + SHOP_AGENT.
 */
@Component
public class OrganizationEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrganizationEventConsumer.class);

    private final OrganizationCascadeUseCase cascade;
    private final ObjectMapper objectMapper;

    public OrganizationEventConsumer(OrganizationCascadeUseCase cascade, ObjectMapper objectMapper) {
        this.cascade = cascade;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = AmqpTopology.QUEUE_IDENTITY_ORG_STATUS)
    public void onOrganizationEvent(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            JsonNode eventTypeNode = node.get("eventType");
            JsonNode eventIdNode = node.get("eventId");
            JsonNode orgIdNode = node.get("organizationId");
            if (eventTypeNode == null || orgIdNode == null) {
                log.warn("Événement organisation ignoré (champs manquants) : {}", payload);
                return;
            }
            String eventType = eventTypeNode.asText();
            String eventId = eventIdNode != null ? eventIdNode.asText() : null;
            UUID organizationId = UUID.fromString(orgIdNode.asText());

            switch (eventType) {
                case SupplierDisabledEvent.EVENT_TYPE -> cascade.onOrganizationDisabled(eventType, organizationId,
                        List.of(RoleCode.SUPPLIER_ADMIN, RoleCode.SUPPLIER_AGENT), eventId);
                case ShopDisabledEvent.EVENT_TYPE -> cascade.onOrganizationDisabled(eventType, organizationId,
                        List.of(RoleCode.SHOP_ADMIN, RoleCode.SHOP_AGENT), eventId);
                default -> log.debug("Événement organisation ignoré : {}", eventType);
            }
        } catch (Exception e) {
            log.error("Échec du traitement de l'événement organisation", e);
            throw new IllegalStateException(e);
        }
    }
}