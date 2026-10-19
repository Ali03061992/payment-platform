package com.paymentplatform.organization.application.usecase;

import com.paymentplatform.organization.domain.model.InvoiceJournal;
import com.paymentplatform.organization.domain.model.Order;
import com.paymentplatform.organization.domain.repository.InvoiceJournalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Émet la ligne du journal des factures pour une commande éligible.
 * Idempotent par commande : la première génération PDF/TEIF attribue le numéro
 * séquentiel (FA-AAAA-NNNNNN, séquence par fournisseur et par année),
 * les régénérations réutilisent la même ligne et le même numéro.
 */
@Service
public class IssueInvoiceUseCase {

    private final InvoiceJournalRepository journal;

    public IssueInvoiceUseCase(InvoiceJournalRepository journal) {
        this.journal = journal;
    }

    @Transactional
    public InvoiceJournal issueIfNeeded(Order order) {
        return journal.findByOrderId(order.getId())
                .orElseGet(() -> journal.save(
                        InvoiceJournal.fromOrder(order, journal.nextInvoiceNumber(order.getSupplierId()))));
    }
}
