package com.paymentplatform.organization.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Règles de facturation par statut : la facture (PDF/TEIF) est émise
 * dès la commande prête pour livraison, avant la livraison.
 * JUnit pur (aucun contexte Spring).
 */
@DisplayName("OrderStatus : disponibilité de la facture par statut.")
class OrderStatusTest {

    @Test
    @DisplayName("Facture disponible à partir de READY_FOR_DELIVERY, jusqu'à ACCEPTED")
    void isInvoiceAvailable_fromReadyForDeliveryOnwards() {
        assertThat(OrderStatus.READY_FOR_DELIVERY.isInvoiceAvailable()).isTrue();
        assertThat(OrderStatus.DELIVERY_ACCEPTED.isInvoiceAvailable()).isTrue();
        assertThat(OrderStatus.IN_DELIVERY.isInvoiceAvailable()).isTrue();
        assertThat(OrderStatus.DELIVERED.isInvoiceAvailable()).isTrue();
        assertThat(OrderStatus.ACCEPTED.isInvoiceAvailable()).isTrue();
    }

    @Test
    @DisplayName("Facture indisponible avant préparation terminée et sur statuts terminaux négatifs")
    void isInvoiceAvailable_beforeReady_returnsFalse() {
        assertThat(OrderStatus.DRAFT.isInvoiceAvailable()).isFalse();
        assertThat(OrderStatus.CONFIRMED.isInvoiceAvailable()).isFalse();
        assertThat(OrderStatus.PREPARING.isInvoiceAvailable()).isFalse();
        assertThat(OrderStatus.DELIVERY_REJECTED.isInvoiceAvailable()).isFalse();
        assertThat(OrderStatus.CANCELLED.isInvoiceAvailable()).isFalse();
        assertThat(OrderStatus.REJECTED.isInvoiceAvailable()).isFalse();
    }

    @Test
    @DisplayName("Variante String : statuts inconnus ou null retournent false")
    void isInvoiceAvailable_string_unknownStatus_returnsFalse() {
        assertThat(OrderStatus.isInvoiceAvailable("READY_FOR_DELIVERY")).isTrue();
        assertThat(OrderStatus.isInvoiceAvailable("DELIVERED")).isTrue();
        assertThat(OrderStatus.isInvoiceAvailable("DRAFT")).isFalse();
        assertThat(OrderStatus.isInvoiceAvailable("UNKNOWN")).isFalse();
        assertThat(OrderStatus.isInvoiceAvailable(null)).isFalse();
    }
}
