package com.paymentplatform.organization.domain.repository;

import com.paymentplatform.organization.domain.model.InvoiceJournal;
import com.paymentplatform.shared.domain.model.PageResult;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceJournalRepository {

    InvoiceJournal save(InvoiceJournal entry);

    /** Ligne déjà émise pour la commande (idempotence). */
    Optional<InvoiceJournal> findByOrderId(UUID orderId);

    /** Lignes portant ce numéro de facture (le numéro est unique par fournisseur). */
    List<InvoiceJournal> findByInvoiceNumber(String invoiceNumber);

    /**
     * Prochain numéro de facture du fournisseur pour l'année en cours
     * (FA-AAAA-NNNNNN), compteur incrémenté de façon atomique.
     */
    String nextInvoiceNumber(UUID supplierId);

    /**
     * Recherche paginée du journal. {@code supplierId} / {@code shopId} null = sans filtre
     * (le contrôle d'accès est porté par l'appelant qui force son propre scope).
     */
    PageResult<InvoiceJournal> search(UUID supplierId, UUID shopId, Integer year, int page, int size);
}
