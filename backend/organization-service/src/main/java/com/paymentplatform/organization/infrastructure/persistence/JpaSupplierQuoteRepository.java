package com.paymentplatform.organization.infrastructure.persistence;

import com.paymentplatform.organization.domain.model.SupplierQuote;
import com.paymentplatform.organization.domain.repository.SupplierQuoteRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class JpaSupplierQuoteRepository implements SupplierQuoteRepository {

    private final SupplierQuoteJpaRepository jpa;

    public JpaSupplierQuoteRepository(SupplierQuoteJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional
    public SupplierQuote save(SupplierQuote quote) {
        return toDomain(jpa.save(toEntity(quote)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SupplierQuote> findById(UUID id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierQuote> findByRequestId(UUID requestId) {
        return jpa.findByRequestIdOrderByVersionAsc(requestId).stream().map(this::toDomain).toList();
    }

    private SupplierQuoteJpaEntity toEntity(SupplierQuote q) {
        SupplierQuoteJpaEntity e = new SupplierQuoteJpaEntity();
        e.setId(q.id());
        e.setRequestId(q.requestId());
        e.setRequestReference(q.requestReference());
        e.setSupplierId(q.supplierId());
        e.setVersion(q.version());
        e.setGlobalDiscount(q.globalDiscount());
        e.setSubtotal(q.subtotal());
        e.setTaxAmount(q.taxAmount());
        e.setTotal(q.total());
        e.setStampAmount(q.stampAmount());
        e.setCurrency(q.currency());
        e.setLinesJson(q.linesJson());
        e.setCreatedBy(q.createdBy());
        e.setCreatedAt(q.createdAt());
        return e;
    }

    private SupplierQuote toDomain(SupplierQuoteJpaEntity e) {
        return SupplierQuote.builder()
                .id(e.getId()).requestId(e.getRequestId()).requestReference(e.getRequestReference())
                .supplierId(e.getSupplierId()).version(e.getVersion())
                .globalDiscount(e.getGlobalDiscount()).subtotal(e.getSubtotal())
                .taxAmount(e.getTaxAmount()).total(e.getTotal()).stampAmount(e.getStampAmount())
                .currency(e.getCurrency()).linesJson(e.getLinesJson())
                .createdBy(e.getCreatedBy()).createdAt(e.getCreatedAt())
                .build();
    }
}
