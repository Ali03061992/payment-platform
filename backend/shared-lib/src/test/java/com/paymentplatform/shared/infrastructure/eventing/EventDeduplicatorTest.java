package com.paymentplatform.shared.infrastructure.eventing;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de EventDeduplicator.
 * Périmètre : idempotence des consommateurs via la table processed_events.
 * Moyens : contexte SpringBootTest, base H2 réelle, insertion et vérification via JdbcClient.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("EventDeduplicator contre H2 : idempotence réelle sans mock.")
class EventDeduplicatorTest {

    @Autowired
    private EventDeduplicator deduplicator;

    @Autowired
    private ProcessedEventRepository repository;

    @Autowired
    private JdbcClient jdbcClient;

    @BeforeEach
    void nettoyer() {
        repository.deleteAll();
    }

    @AfterEach
    void purgerApresChaqueTest() {
        // markProcessed utilise REQUIRES_NEW : les lignes survivent au rollback, purge explicite.
        repository.deleteAll();
    }

    @Test
    @DisplayName("Premier appel à markProcessed retourne vrai et persiste la ligne en base.")
    void markProcessed_premierAppel_retourneVraiEtPersiste() {
        String eventId = "evt-premier-" + System.nanoTime();

        boolean premier = deduplicator.markProcessed(eventId);

        assertThat(premier).isTrue();
        assertThat(repository.existsById(eventId)).isTrue();
        Integer lignes = jdbcClient.sql("SELECT COUNT(*) FROM processed_events WHERE event_id = :id")
                .param("id", eventId)
                .query(Integer.class)
                .single();
        assertThat(lignes).isEqualTo(1);
    }

    @Test
    @DisplayName("Second appel avec le même identifiant retourne faux (doublon détecté).")
    void markProcessed_doublon_retourneFaux() {
        String eventId = "evt-dup-" + System.nanoTime();

        assertThat(deduplicator.markProcessed(eventId)).isTrue();
        assertThat(deduplicator.markProcessed(eventId)).isFalse();
        assertThat(deduplicator.isProcessed(eventId)).isTrue();
    }

    @Test
    @DisplayName("isProcessed retourne vrai après marquage et faux pour un identifiant inconnu.")
    void isProcessed_apresMarquage_retourneVraiSinonFaux() {
        String connu = "evt-connu-" + System.nanoTime();
        String inconnu = "evt-inconnu-" + System.nanoTime();

        assertThat(deduplicator.isProcessed(inconnu)).isFalse();

        deduplicator.markProcessed(connu);

        assertThat(deduplicator.isProcessed(connu)).isTrue();
        assertThat(deduplicator.isProcessed(inconnu)).isFalse();
    }

    @Test
    @DisplayName("Deux identifiants distincts sont marqués indépendamment.")
    void markProcessed_identifiantsDistincts_marquesIndependamment() {
        String premier = "evt-a-" + System.nanoTime();
        String second = "evt-b-" + System.nanoTime();

        assertThat(deduplicator.markProcessed(premier)).isTrue();
        assertThat(deduplicator.markProcessed(second)).isTrue();
        assertThat(repository.count()).isEqualTo(2L);
    }
}
