package com.paymentplatform.shared.infrastructure.outbox;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de OutboxPublisher.
 * Périmètre : construction du message AMQP et délégation au broker.
 * Moyens : JUnit pur avec un faux RabbitTemplate manuel (zéro simulation, base non requise).
 */
class OutboxPublisherTest {

    /** Faux manuel : capture les appels convertAndSend sans framework de mock. */
    static final class RabbitTemplateCapturant extends RabbitTemplate {
        record Envoi(String exchange, String routingKey, Message message) {
        }

        private final List<Envoi> envois = new ArrayList<>();

        @Override
        public void convertAndSend(String exchange, String routingKey, Object message) {
            envois.add(new Envoi(exchange, routingKey, (Message) message));
        }

        List<Envoi> envois() {
            return List.copyOf(envois);
        }
    }

    @Test
    @DisplayName("La publication envoie l'échange, la clé de routage et un message JSON identifié.")
    void publish_messageJson_envoyeAvecBonnesMetadonnees() {
        RabbitTemplateCapturant gabarit = new RabbitTemplateCapturant();
        OutboxPublisher editeur = new OutboxPublisher(gabarit);

        editeur.publish("payment.events", "payment.created", "evt-123", "{\"key\":\"value\"}");

        assertThat(gabarit.envois()).hasSize(1);
        RabbitTemplateCapturant.Envoi envoi = gabarit.envois().get(0);
        assertThat(envoi.exchange()).isEqualTo("payment.events");
        assertThat(envoi.routingKey()).isEqualTo("payment.created");
        assertThat(envoi.message().getMessageProperties().getMessageId()).isEqualTo("evt-123");
        assertThat(envoi.message().getMessageProperties().getContentType())
                .isEqualTo(MessageProperties.CONTENT_TYPE_JSON);
        assertThat(new String(envoi.message().getBody(), StandardCharsets.UTF_8))
                .isEqualTo("{\"key\":\"value\"}");
    }

    @Test
    @DisplayName("Deux publications successives produisent deux envois distincts.")
    void publish_deuxAppels_produitDeuxEnvois() {
        RabbitTemplateCapturant gabarit = new RabbitTemplateCapturant();
        OutboxPublisher editeur = new OutboxPublisher(gabarit);

        editeur.publish("payment.events", "payment.created", "evt-1", "{}");
        editeur.publish("organization.events", "order.created", "evt-2", "{}");

        assertThat(gabarit.envois()).hasSize(2);
        assertThat(gabarit.envois())
                .extracting(RabbitTemplateCapturant.Envoi::exchange)
                .containsExactly("payment.events", "organization.events");
    }
}
