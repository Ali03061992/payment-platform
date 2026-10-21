package com.paymentplatform.organization.application.service;

import com.paymentplatform.organization.domain.model.Order;
import com.paymentplatform.organization.domain.model.OrderItem;
import com.paymentplatform.organization.domain.model.PaymentTerms;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
/**
 * Tests de InvoicePdfServiceTest.
 * Perimetre : comportement de InvoicePdfService.
 * Moyens : JUnit pur (AssertJ).
 */

class InvoicePdfServiceTest {

    private final InvoicePdfService service = new InvoicePdfService();

    private Order order() {
        Order order = Order.create(
                UUID.fromString("00000000-0000-0000-0000-000000000020"),
                UUID.fromString("00000000-0000-0000-0000-000000000010"),
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "SHOP_ADMIN", "MANUAL", false, "TND", PaymentTerms.IMMEDIATE);
        org.springframework.test.util.ReflectionTestUtils.setField(order, "status", "ACCEPTED");
        org.springframework.test.util.ReflectionTestUtils.setField(order, "subtotal", new BigDecimal("100.00"));
        org.springframework.test.util.ReflectionTestUtils.setField(order, "taxRate", new BigDecimal("19.00"));
        org.springframework.test.util.ReflectionTestUtils.setField(order, "taxAmount", new BigDecimal("19.00"));
        org.springframework.test.util.ReflectionTestUtils.setField(order, "total", new BigDecimal("119.00"));
        return order;
    }

    private OrderItem item(Order order) {
        OrderItem item = new OrderItem();
        org.springframework.test.util.ReflectionTestUtils.setField(item, "orderId", order.getId());
        org.springframework.test.util.ReflectionTestUtils.setField(item, "productId", UUID.randomUUID());
        org.springframework.test.util.ReflectionTestUtils.setField(item, "productName", "Widget");
        org.springframework.test.util.ReflectionTestUtils.setField(item, "quantity", 2);
        org.springframework.test.util.ReflectionTestUtils.setField(item, "unitPrice", new BigDecimal("50.00"));
        org.springframework.test.util.ReflectionTestUtils.setField(item, "discount", BigDecimal.ZERO);
        org.springframework.test.util.ReflectionTestUtils.setField(item, "lineTotal", new BigDecimal("100.00"));
        return item;
    }

    @Test
    void generateInvoicePdf_validOrder_returnsPdfBytes() {
        Order order = order();

        byte[] pdf = service.generateInvoicePdf(order, List.of(item(order)),
                "Fournisseur Test", "Boutique Test");

        assertThat(pdf).isNotEmpty();
        assertThat(pdf[0]).isEqualTo((byte) '%');
        assertThat(pdf[1]).isEqualTo((byte) 'P');
        assertThat(pdf[2]).isEqualTo((byte) 'D');
        assertThat(pdf[3]).isEqualTo((byte) 'F');
    }

    @Test
    void generateInvoicePdf_nullOrgNames_returnsPdfBytes() {
        Order order = order();

        byte[] pdf = service.generateInvoicePdf(order, List.of(item(order)), (String) null, (String) null);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
    }

    @Test
    void generatePurchaseOrderPdf_validOrder_returnsPdfBytes() {
        Order order = order();

        byte[] pdf = service.generatePurchaseOrderPdf(order, List.of(item(order)),
                InvoicePdfService.InvoiceParty.named("Fournisseur Test"), InvoicePdfService.InvoiceParty.named("Boutique Test"),
                "https://nextera.payment.tn/dashboard/shop/orders/CMD-2026-001");

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
    }

    @Test
    void generateInvoicePdf_withPartiesAndBreakdown_returnsPdfBytes() {
        Order order = order();
        org.springframework.test.util.ReflectionTestUtils.setField(order, "vatBreakdown", "19|1000|190;7|500|35");
        org.springframework.test.util.ReflectionTestUtils.setField(order, "fodecTotal", new BigDecimal("10.00"));
        org.springframework.test.util.ReflectionTestUtils.setField(order, "stampAmount", new BigDecimal("1.000"));
        org.springframework.test.util.ReflectionTestUtils.setField(order, "total", new BigDecimal("1735.00"));

        var supplier = new InvoicePdfService.InvoiceParty("Fournisseur SARL", "Rue de l'Industrie, Tunis",
                "1234567A/A/M/000", "PAT-001", "ASSUJETTI", null, null);
        var buyer = new InvoicePdfService.InvoiceParty("Boutique", "Avenue Habib Bourguiba, Tunis",
                "7654321B/A/M/001", null, "ASSUJETTI", null, null);

        OrderItem line = item(order);
        org.springframework.test.util.ReflectionTestUtils.setField(line, "vatRate", new BigDecimal("19"));

        byte[] pdf = service.generateInvoicePdf(order, List.of(line), supplier, buyer);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
    }

    @Test
    void generateInvoicePdf_suspendedMention_returnsPdfBytes() {
        Order order = order();
        org.springframework.test.util.ReflectionTestUtils.setField(order, "taxAmount", BigDecimal.ZERO);
        org.springframework.test.util.ReflectionTestUtils.setField(order, "total", new BigDecimal("100.00"));
        org.springframework.test.util.ReflectionTestUtils.setField(order, "vatBreakdown", "0|100|0");
        org.springframework.test.util.ReflectionTestUtils.setField(order, "fiscalMention",
                "Vente en suspension de la taxe sur la valeur ajoutée suivant attestation n°ATT-1 du 01/01/2026 (art. 11 du Code TVA)");

        var supplier = new InvoicePdfService.InvoiceParty("Fournisseur SARL", null,
                "1234567A/A/M/000", null, "ASSUJETTI", null, null);
        var buyer = new InvoicePdfService.InvoiceParty("Client Suspendu", null,
                "1111111C/A/M/000", null, "SUSPENDU", "ATT-1", java.time.LocalDate.of(2026, 1, 1));

        byte[] pdf = service.generateInvoicePdf(order, List.of(item(order)), supplier, buyer);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
    }
}
