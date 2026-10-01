package com.paymentplatform.notification.infrastructure.email;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de EmailTemplateService.
 * Périmètre : génération des gabarits HTML transactionnels.
 * Moyens : JUnit pur, instance réelle sans Spring ni simulation.
 */
@DisplayName("Gabarits d'emails : contenu transactionnel en français.")
class EmailTemplateServiceTests {

    private final EmailTemplateService gabarits = new EmailTemplateService();

    @Test
    @DisplayName("La confirmation de commande contient le destinataire, la référence et le détail.")
    void buildOrderConfirmation_parametres_contientReferences() {
        String html = gabarits.buildOrderConfirmation("Amina", "ORD-2026-001", "2 × Huile d'olive");

        assertThat(html).contains("Amina");
        assertThat(html).contains("ORD-2026-001");
        assertThat(html).contains("2 × Huile d'olive");
        assertThat(html).contains("Payment Platform");
    }

    @Test
    @DisplayName("La notification de livraison contient le destinataire et les informations.")
    void buildDeliveryNotification_parametres_contientInformations() {
        String html = gabarits.buildDeliveryNotification("Yassine", "ORD-2026-002", "Livraison demain");

        assertThat(html).contains("Yassine");
        assertThat(html).contains("ORD-2026-002");
        assertThat(html).contains("Livraison demain");
    }

    @Test
    @DisplayName("Le reçu de paiement contient le montant, la devise et les références.")
    void buildPaymentReceipt_parametres_contientMontantEtReferences() {
        String html = gabarits.buildPaymentReceipt("Sarra", "PAY-2026-009", "150.00", "TND", "ORD-2026-003");

        assertThat(html).contains("Sarra");
        assertThat(html).contains("150.00");
        assertThat(html).contains("TND");
        assertThat(html).contains("PAY-2026-009");
        assertThat(html).contains("ORD-2026-003");
    }

    @Test
    @DisplayName("L'invitation d'agent contient l'organisation et le lien d'activation.")
    void buildAgentInvitation_parametres_contientLien() {
        String html = gabarits.buildAgentInvitation("Mehdi", "Superette Tunis", "https://app/invite/abc");

        assertThat(html).contains("Mehdi");
        assertThat(html).contains("Superette Tunis");
        assertThat(html).contains("https://app/invite/abc");
    }
}
