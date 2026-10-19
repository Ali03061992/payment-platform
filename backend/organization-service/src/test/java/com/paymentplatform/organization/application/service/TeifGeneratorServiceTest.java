package com.paymentplatform.organization.application.service;

import com.paymentplatform.organization.domain.model.Order;
import com.paymentplatform.organization.domain.model.OrderItem;
import com.paymentplatform.organization.domain.model.PaymentTerms;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Générateur TEIF v1.8.8 (facture électronique TTN/El Fatoora).
 * JUnit pur (aucun contexte Spring).
 */
@DisplayName("TEIF v1.8.8 : facture électronique XML.")
class TeifGeneratorServiceTest {

    private Order order() {
        Order order = Order.create(
                UUID.fromString("00000000-0000-0000-0000-000000000020"),
                UUID.fromString("00000000-0000-0000-0000-000000000010"),
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "SHOP_ADMIN", "MANUAL", false, "TND", PaymentTerms.IMMEDIATE);
        ReflectionTestUtils.setField(order, "status", "DELIVERED");
        ReflectionTestUtils.setField(order, "subtotal", new BigDecimal("1500.00"));
        ReflectionTestUtils.setField(order, "taxAmount", new BigDecimal("262.50"));
        ReflectionTestUtils.setField(order, "total", new BigDecimal("1762.50"));
        ReflectionTestUtils.setField(order, "stampAmount", new BigDecimal("1.000"));
        ReflectionTestUtils.setField(order, "vatBreakdown", "19|1000|190;7|500|35;13|62.50|8.125");
        ReflectionTestUtils.setField(order, "createdAt", Instant.parse("2026-10-17T10:00:00Z"));
        return order;
    }

    private OrderItem item(UUID orderId) {
        OrderItem item = new OrderItem();
        ReflectionTestUtils.setField(item, "orderId", orderId);
        ReflectionTestUtils.setField(item, "productId", UUID.randomUUID());
        ReflectionTestUtils.setField(item, "productRef", "SKU-1");
        ReflectionTestUtils.setField(item, "productName", "Widget & Fils <test>");
        ReflectionTestUtils.setField(item, "quantity", 2);
        ReflectionTestUtils.setField(item, "unitPrice", new BigDecimal("500.00"));
        ReflectionTestUtils.setField(item, "htUnitPrice", new BigDecimal("500.00"));
        ReflectionTestUtils.setField(item, "vatRate", new BigDecimal("19"));
        ReflectionTestUtils.setField(item, "lineTotal", new BigDecimal("1000.00"));
        return item;
    }

    private TeifGeneratorService.TeifParty supplier() {
        return new TeifGeneratorService.TeifParty("Fournisseur Test", "1234567A/A/M/000", "Tunis");
    }

    private TeifGeneratorService.TeifParty buyer() {
        return new TeifGeneratorService.TeifParty("Boutique Test", "7654321B/C/M/001", "Sfax");
    }

    @Test
    @DisplayName("MF normalisés 13 caractères, en-tête TTN, codes I-*")
    void header_normalizedMf() {
        String xml = TeifGeneratorService.buildInvoice(order(), List.of(item(UUID.randomUUID())), supplier(), buyer());
        assertThat(xml).startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        assertThat(xml).contains("<TEIF version=\"1.8.8\" controlingAgency=\"TTN\">");
        assertThat(xml).contains("<MessageSenderIdentifier type=\"I-01\">1234567AAM000</MessageSenderIdentifier>");
        assertThat(xml).contains("<MessageRecieverIdentifier type=\"I-01\">7654321BCM001</MessageRecieverIdentifier>");
        assertThat(xml).contains("<DocumentType code=\"I-11\">Facture</DocumentType>");
        assertThat(xml).contains("<DateText format=\"ddMMyy\">171026</DateText>");
        assertThat(xml).contains("<Partner function=\"I-62\">");
        assertThat(xml).contains("<Partner function=\"I-64\">");
        assertThat(xml).contains("<Tax code=\"I-1602\" rate=\"19\"/>");
    }

    @Test
    @DisplayName("Totaux 3 décimales : I-176 HT, I-181 taxes, I-180 TTC, timbre séparé, ventilation par taux")
    void totals_threeDecimals() {
        String xml = TeifGeneratorService.buildInvoice(order(), List.of(item(UUID.randomUUID())), supplier(), buyer());
        assertThat(xml).contains("<Moa amountTypeCode=\"I-176\" currencyIdentifier=\"TND\">1500.000</Moa>");
        assertThat(xml).contains("<Moa amountTypeCode=\"I-181\" currencyIdentifier=\"TND\">262.500</Moa>");
        assertThat(xml).contains("<Moa amountTypeCode=\"I-180\" currencyIdentifier=\"TND\">1762.500</Moa>");
        assertThat(xml).contains("<TimbreFiscal currencyIdentifier=\"TND\">1.000</TimbreFiscal>");
        assertThat(xml).contains("<TaxDetail rate=\"19\">");
        assertThat(xml).contains("<BaseAmount>1000.000</BaseAmount>");
        assertThat(xml).contains("<TaxAmount>190.000</TaxAmount>");
    }

    @Test
    @DisplayName("Échappement XML des désignations, RefTtnVal vide puis remplie")
    void escaping_andRef() {
        Order o = order();
        String xml = TeifGeneratorService.buildInvoice(o, List.of(item(UUID.randomUUID())), supplier(), buyer());
        assertThat(xml).contains("<Imd>Widget &amp; Fils &lt;test&gt;</Imd>");
        assertThat(xml).contains("<RefTtnVal></RefTtnVal>");
        ReflectionTestUtils.setField(o, "externalReference", "TTN-2026-0001");
        String xml2 = TeifGeneratorService.buildInvoice(o, List.of(item(UUID.randomUUID())), supplier(), buyer());
        assertThat(xml2).contains("<RefTtnVal>TTN-2026-0001</RefTtnVal>");
    }

    @Test
    @DisplayName("Numéro de journal (FA-AAAA-NNNNNN) en DocumentIdentifier")
    void invoiceNumber_overridesDocumentIdentifier() {
        String xml = TeifGeneratorService.buildInvoice(order(), List.of(item(UUID.randomUUID())),
                supplier(), buyer(), "FA-2026-000007");
        assertThat(xml).contains("<DocumentIdentifier>FA-2026-000007</DocumentIdentifier>");
        assertThat(xml).doesNotContain("<DocumentIdentifier>" + order().getReference() + "</DocumentIdentifier>");
    }

    @Test
    @DisplayName("MF non convertible => erreur explicite (pas de XML invalide)")
    void invalidMf_throws() {
        var bad = new TeifGeneratorService.TeifParty("X", "ABC", "Tunis");
        assertThatThrownBy(() -> TeifGeneratorService.buildInvoice(order(),
                List.of(item(UUID.randomUUID())), bad, buyer()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("TEIF");
    }
}
