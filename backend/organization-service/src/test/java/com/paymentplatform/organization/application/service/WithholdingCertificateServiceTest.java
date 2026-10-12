package com.paymentplatform.organization.application.service;

import com.paymentplatform.organization.application.dto.WithholdingCertificateResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Certificat RAS rattaché à la facture : XML TEJ-ready, PDF avec QR.
 * JUnit pur (aucun contexte Spring).
 */
@DisplayName("Certificat de retenue (commande) : XML, PDF et QR.")
class WithholdingCertificateServiceTest {

    private WithholdingCertificateResponse cert() {
        return new WithholdingCertificateResponse(
                UUID.randomUUID(), UUID.randomUUID(), "CRT-202610-AB12CD34",
                UUID.randomUUID(), "Boutique Test", "1234567A/A/M/000", "Tunis",
                UUID.randomUUID(), "Fournisseur Test", "7654321B/A/M/000", "Sfax",
                "MARCHANDISES", new BigDecimal("11900"), new BigDecimal("1.5"),
                new BigDecimal("178.5000"), new BigDecimal("11721.5000"), "TND",
                "ORD-001", LocalDate.of(2026, 7, 15), "2026-07", LocalDate.of(2026, 8, 31),
                "READY", null, Instant.now());
    }

    @Test
    @DisplayName("XML : mentions art. 55 + arrêté, facture en référence, 3 décimales")
    void xml_containsLegalMentions() {
        String xml = WithholdingXmlService.buildCertificate(cert());
        assertThat(xml).contains("<numero>CRT-202610-AB12CD34</numero>");
        assertThat(xml).contains("<matriculeFiscal>1234567A/A/M/000</matriculeFiscal>");
        assertThat(xml).contains("<facture>");
        assertThat(xml).contains("<reference>ORD-001</reference>");
        assertThat(xml).contains("<brut>11900.000</brut>");
        assertThat(xml).contains("<taux>1.500</taux>");
        assertThat(xml).contains("<retenue>178.500</retenue>");
        assertThat(xml).contains("<echeance>2026-08-31</echeance>");
    }

    @Test
    @DisplayName("XML lot : commentaire déposant + enveloppe")
    void xml_batch() {
        String xml = WithholdingXmlService.buildBatch(List.of(cert()), "TEJ-9");
        assertThat(xml).startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        assertThat(xml).contains("<!-- deposant TEJ: TEJ-9 -->");
        assertThat(xml).contains("<certificatsRetenue");
    }

    @Test
    @DisplayName("PDF : binaire valide avec QR et référence")
    void pdf_validBytes() {
        byte[] pdf = new WithholdingCertificatePdfService().generateCertificatePdf(cert());
        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
        assertThat(WithholdingCertificatePdfService.qrPayload(cert())).startsWith("TEJ:CERT:CRT-202610-AB12CD34");
    }
}
