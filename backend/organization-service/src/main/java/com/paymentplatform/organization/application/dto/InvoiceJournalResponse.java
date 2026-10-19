package com.paymentplatform.organization.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Ligne du journal des factures (numéro, commande, parties, montants). */
public record InvoiceJournalResponse(
        UUID id,
        String invoiceNumber,
        UUID orderId,
        String orderReference,
        UUID supplierId,
        String supplierName,
        UUID shopId,
        String shopName,
        BigDecimal totalTtc,
        BigDecimal vatAmount,
        String currency,
        Instant issuedAt) {
}
