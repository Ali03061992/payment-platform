package com.paymentplatform.organization.application.service;

import com.lowagie.text.pdf.PdfReader;
import com.paymentplatform.organization.domain.model.Order;
import com.paymentplatform.organization.domain.model.OrderItem;
import com.paymentplatform.organization.domain.model.PaymentTerms;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mise en page facture : une ligne => UNE page (totaux + mentions + signatures),
 * jamais de bloc totaux déchiré, signatures toujours présentes.
 * JUnit pur (AssertJ + lecture iText).
 */
@DisplayName("Facture PDF : mise en page pleine page.")
class InvoicePdfLayoutTest {

    private final InvoicePdfService service = new InvoicePdfService();

    private Order order(String subtotal, String tax, String total, String breakdown) {
        Order order = Order.create(
                UUID.fromString("00000000-0000-0000-0000-000000000020"),
                UUID.fromString("00000000-0000-0000-0000-000000000010"),
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "SHOP_ADMIN", "MANUAL", false, "TND", PaymentTerms.IMMEDIATE);
        org.springframework.test.util.ReflectionTestUtils.setField(order, "status", "ACCEPTED");
        org.springframework.test.util.ReflectionTestUtils.setField(order, "subtotal", new BigDecimal(subtotal));
        org.springframework.test.util.ReflectionTestUtils.setField(order, "taxAmount", new BigDecimal(tax));
        org.springframework.test.util.ReflectionTestUtils.setField(order, "fodecTotal", BigDecimal.ZERO);
        org.springframework.test.util.ReflectionTestUtils.setField(order, "stampAmount", new BigDecimal("1.000"));
        org.springframework.test.util.ReflectionTestUtils.setField(order, "total", new BigDecimal(total));
        org.springframework.test.util.ReflectionTestUtils.setField(order, "vatBreakdown", breakdown);
        return order;
    }

    private OrderItem item(Order order, String name, int qty, String price, String vat, boolean ttc) {
        OrderItem item = new OrderItem();
        org.springframework.test.util.ReflectionTestUtils.setField(item, "orderId", order.getId());
        org.springframework.test.util.ReflectionTestUtils.setField(item, "productId", UUID.randomUUID());
        org.springframework.test.util.ReflectionTestUtils.setField(item, "productName", name);
        org.springframework.test.util.ReflectionTestUtils.setField(item, "quantity", qty);
        org.springframework.test.util.ReflectionTestUtils.setField(item, "unitPrice", new BigDecimal(price));
        org.springframework.test.util.ReflectionTestUtils.setField(item, "discount", BigDecimal.ZERO);
        org.springframework.test.util.ReflectionTestUtils.setField(item, "lineTotal", new BigDecimal(price).multiply(BigDecimal.valueOf(qty)));
        org.springframework.test.util.ReflectionTestUtils.setField(item, "vatRate", new BigDecimal(vat));
        item.setPriceTtc(ttc);
        return item;
    }

    private InvoicePdfService.InvoiceParty party(String name, String mf) {
        return new InvoicePdfService.InvoiceParty(name, "Adresse Test, Tunis", mf, null, "ASSUJETTI", null, null);
    }

    private String textOf(byte[] pdf) {
        StringBuilder sb = new StringBuilder();
        PdfReader reader;
        try {
            reader = new PdfReader(pdf);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        sb.append("PAGES=").append(reader.getNumberOfPages()).append("\n");
        com.lowagie.text.pdf.parser.PdfTextExtractor extractor =
                new com.lowagie.text.pdf.parser.PdfTextExtractor(reader);
        for (int i = 1; i <= reader.getNumberOfPages(); i++) {
            try {
                sb.append(extractor.getTextFromPage(i));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            sb.append("\n---\n");
        }
        reader.close();
        return sb.toString();
    }

    @Test
    @DisplayName("Une ligne : une seule page avec totaux, mentions et signatures")
    void singleLine_singlePage() {
        Order order = order("100.00", "19.00", "119.00", "19|100|19");
        byte[] pdf = service.generateInvoicePdf(order,
                List.of(item(order, "Widget", 2, "50.00", "19", false)),
                party("Fournisseur", "1234567A/A/M/000"), party("Boutique", "7654321B/A/M/001"));

        String text = textOf(pdf);
        assertThat(text).startsWith("PAGES=1\n");
        assertThat(text).contains("NET À PAYER");
        assertThat(text).contains("Timbre fiscal");
        assertThat(text).contains("Signature du fournisseur");
        assertThat(text).contains("Signature du client");
        assertThat(text).contains("Mentions légales");
    }

    @Test
    @DisplayName("Ligne TTC : mention TTC et totaux cohérents")
    void ttcLine_marked() {
        Order order = order("1000.00", "190.00", "1190.00", "19|1000|190");
        byte[] pdf = service.generateInvoicePdf(order,
                List.of(item(order, "Produit TTC", 1, "1190.00", "19", true)),
                party("Fournisseur", "1234567A/A/M/000"), party("Boutique", "7654321B/A/M/001"));

        String text = textOf(pdf);
        assertThat(text).contains("TTC");
        assertThat(text).contains("NET À PAYER");
    }

    @Test
    @DisplayName("Trente lignes : pagination avec totaux groupés et signatures en fin")
    void manyLines_totalsKeptTogether() {
        Order order = order("300.00", "57.00", "357.00", "19|300|57");
        List<OrderItem> items = new ArrayList<>();
        for (int i = 1; i <= 30; i++) {
            items.add(item(order, "Produit " + i, 1, "10.00", "19", false));
        }
        byte[] pdf = service.generateInvoicePdf(order, items,
                party("Fournisseur", "1234567A/A/M/000"), party("Boutique", "7654321B/A/M/001"));

        String text = textOf(pdf);
        int pages = Integer.parseInt(text.split("\n")[0].substring("PAGES=".length()));
        assertThat(pages).isGreaterThanOrEqualTo(2);
        String[] parts = text.split("---\n");
        String lastPage = parts[parts.length - 1].isBlank() ? parts[parts.length - 2] : parts[parts.length - 1];
        assertThat(lastPage).contains("NET À PAYER");
        assertThat(lastPage).contains("Timbre fiscal");
        assertThat(lastPage).contains("Signature du fournisseur");
    }
}
