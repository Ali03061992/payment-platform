package com.paymentplatform.organization.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de DocumentQrPayload.
 * Perimetre : serialisation du QR commun aux documents metier.
 * Moyens : JUnit pur (AssertJ + Jackson).
 */
class DocumentQrPayloadTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void of_serializesVersionTypeAndReference() throws Exception {
        String json = DocumentQrPayload.of(DocumentQrPayload.TYPE_ORDER, "CMD-2026-0001");

        JsonNode node = MAPPER.readTree(json);
        assertThat(node.get("v").asInt()).isEqualTo(1);
        assertThat(node.get("type").asText()).isEqualTo("ORDER");
        assertThat(node.get("reference").asText()).isEqualTo("CMD-2026-0001");
    }

    @Test
    void of_nullReference_serializesEmptyString() throws Exception {
        String json = DocumentQrPayload.of(DocumentQrPayload.TYPE_INVOICE, null);

        JsonNode node = MAPPER.readTree(json);
        assertThat(node.get("type").asText()).isEqualTo("INVOICE");
        assertThat(node.get("reference").asText()).isEmpty();
    }

    @Test
    void of_referenceWithSpecialCharacters_staysValidJson() throws Exception {
        String json = DocumentQrPayload.of(DocumentQrPayload.TYPE_QUOTE, "DEV \"2026\"\\01");

        JsonNode node = MAPPER.readTree(json);
        assertThat(node.get("reference").asText()).isEqualTo("DEV \"2026\"\\01");
    }

    @Test
    void typeConstants_matchScannerContract() {
        assertThat(DocumentQrPayload.TYPE_ORDER).isEqualTo("ORDER");
        assertThat(DocumentQrPayload.TYPE_INVOICE).isEqualTo("INVOICE");
        assertThat(DocumentQrPayload.TYPE_QUOTE).isEqualTo("QUOTE");
        assertThat(DocumentQrPayload.TYPE_DELIVERY).isEqualTo("DELIVERY");
        assertThat(DocumentQrPayload.TYPE_PAYMENT).isEqualTo("PAYMENT");
        assertThat(DocumentQrPayload.TYPE_DISPUTE).isEqualTo("DISPUTE");
        assertThat(DocumentQrPayload.TYPE_WITHHOLDING_CERTIFICATE).isEqualTo("WITHHOLDING_CERTIFICATE");
    }
}
