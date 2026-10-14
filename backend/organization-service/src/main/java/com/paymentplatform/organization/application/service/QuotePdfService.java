package com.paymentplatform.organization.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentplatform.organization.application.service.InvoicePdfService.InvoiceParty;
import com.paymentplatform.organization.domain.model.Order;
import com.paymentplatform.organization.domain.model.OrderItem;
import com.paymentplatform.organization.domain.model.OrderQuote;
import com.paymentplatform.organization.domain.model.PaymentTerms;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * PDF d'un devis versionné : reconstruit l'état figé (lignes + totaux) depuis
 * le snapshot JSON, puis réutilise la mise en page facture en mode DEVIS.
 */
@org.springframework.stereotype.Service
public class QuotePdfService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final InvoicePdfService invoicePdfService;

    public QuotePdfService() {
        this(new InvoicePdfService());
    }

    public QuotePdfService(InvoicePdfService invoicePdfService) {
        this.invoicePdfService = invoicePdfService;
    }

    public byte[] generateQuotePdf(OrderQuote quote, InvoiceParty supplier, InvoiceParty buyer,
                                   UUID shopId) {
        List<OrderItem> items = rebuildItems(quote);
        Order display = Order.create(quote.supplierId(), shopId,
                quote.createdBy(), "SUPPLIER_ADMIN", "MANUAL", false, quote.currency(),
                PaymentTerms.IMMEDIATE);
        display.setGlobalDiscount(quote.globalDiscount() != null ? quote.globalDiscount() : BigDecimal.ZERO);
        display.recalculateTotals(items);
        display.setStampAmount(quote.stampAmount() != null ? quote.stampAmount() : BigDecimal.ZERO);
        return invoicePdfService.generateQuotePdf(display, items, supplier, buyer,
                quote.version(), quote.orderReference());
    }

    private List<OrderItem> rebuildItems(OrderQuote quote) {
        List<OrderItem> items = new ArrayList<>();
        try {
            JsonNode root = MAPPER.readTree(quote.linesJson() != null ? quote.linesJson() : "[]");
            if (root.isArray()) {
                for (JsonNode n : root) {
                    OrderItem item = new OrderItem();
                    item.setOrderId(quote.orderId());
                    item.setProductRef(text(n, "ref"));
                    item.setProductName(text(n, "name"));
                    item.setQuantity(n.has("qty") ? n.get("qty").asInt(1) : 1);
                    item.setUnitPrice(num(n, "unitPrice"));
                    item.setDiscount(num(n, "discount"));
                    item.setVatRate(num(n, "vatRate"));
                    items.add(item);
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Snapshot de devis illisible", e);
        }
        return items;
    }

    private String text(JsonNode n, String field) {
        return n.has(field) && !n.get(field).isNull() ? n.get(field).asText("") : "";
    }

    private BigDecimal num(JsonNode n, String field) {
        try {
            return n.has(field) && !n.get(field).isNull()
                    ? new BigDecimal(n.get(field).asText("0")) : BigDecimal.ZERO;
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}
