package com.paymentplatform.organization.infrastructure.persistence;

import com.paymentplatform.organization.domain.model.InvoiceJournal;
import com.paymentplatform.organization.domain.repository.InvoiceJournalRepository;
import com.paymentplatform.shared.domain.model.PageResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Component
public class JpaInvoiceJournalRepository implements InvoiceJournalRepository {

    private final InvoiceJournalJpaRepository journal;
    private final InvoiceSequenceJpaRepository sequences;

    public JpaInvoiceJournalRepository(InvoiceJournalJpaRepository journal,
                                       InvoiceSequenceJpaRepository sequences) {
        this.journal = journal;
        this.sequences = sequences;
    }

    @Override
    @Transactional
    public InvoiceJournal save(InvoiceJournal entry) {
        InvoiceJournalJpaEntity entity = toEntity(entry);
        if (entity.getId() == null) {
            entity.setId(java.util.UUID.randomUUID());
        }
        if (entity.getIssuedAt() == null) {
            entity.setIssuedAt(java.time.Instant.now());
        }
        return toDomain(journal.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<InvoiceJournal> findByOrderId(UUID orderId) {
        return journal.findByOrderId(orderId).map(this::toDomain);
    }

    @Override
    @Transactional
    public String nextInvoiceNumber(UUID supplierId) {
        int year = LocalDate.now().getYear();
        String supplierKey = supplierId.toString();
        InvoiceSequenceJpaEntity seq = sequences.findForUpdate(supplierKey, year)
                .orElseGet(() -> {
                    InvoiceSequenceJpaEntity created = new InvoiceSequenceJpaEntity();
                    created.setSupplierId(supplierKey);
                    created.setSeqYear(year);
                    created.setLastNumber(0);
                    return sequences.save(created);
                });
        seq.setLastNumber(seq.getLastNumber() + 1);
        sequences.save(seq);
        return String.format("FA-%d-%06d", year, seq.getLastNumber());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<InvoiceJournal> search(UUID supplierId, UUID shopId, Integer year, int page, int size) {
        Page<InvoiceJournalJpaEntity> result =
                journal.search(supplierId, shopId, year, PageRequest.of(page, size));
        return new PageResult<>(result.getContent().stream().map(this::toDomain).toList(),
                result.getTotalElements());
    }

    private InvoiceJournalJpaEntity toEntity(InvoiceJournal e) {
        InvoiceJournalJpaEntity entity = new InvoiceJournalJpaEntity();
        entity.setId(e.id());
        entity.setInvoiceNumber(e.invoiceNumber());
        entity.setOrderId(e.orderId());
        entity.setOrderReference(e.orderReference());
        entity.setSupplierId(e.supplierId());
        entity.setShopId(e.shopId());
        entity.setCurrency(e.currency());
        entity.setTotalTtc(e.totalTtc());
        entity.setVatAmount(e.vatAmount());
        entity.setIssuedAt(e.issuedAt());
        return entity;
    }

    private InvoiceJournal toDomain(InvoiceJournalJpaEntity e) {
        return InvoiceJournal.builder()
                .id(e.getId())
                .invoiceNumber(e.getInvoiceNumber())
                .orderId(e.getOrderId())
                .orderReference(e.getOrderReference())
                .supplierId(e.getSupplierId())
                .shopId(e.getShopId())
                .currency(e.getCurrency())
                .totalTtc(e.getTotalTtc())
                .vatAmount(e.getVatAmount())
                .issuedAt(e.getIssuedAt())
                .build();
    }
}
