package com.paymentplatform.organization.infrastructure.persistence;

import com.paymentplatform.organization.domain.model.QuoteRequest;
import com.paymentplatform.organization.domain.repository.QuoteRequestRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class JpaQuoteRequestRepository implements QuoteRequestRepository {

    private final QuoteRequestJpaRepository jpa;

    public JpaQuoteRequestRepository(QuoteRequestJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional
    public QuoteRequest save(QuoteRequest request) {
        return toDomain(jpa.save(toEntity(request)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<QuoteRequest> findById(UUID id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<QuoteRequest> findByReference(String reference) {
        return jpa.findByReference(reference).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuoteRequest> findByShopId(UUID shopId) {
        return jpa.findByShopIdOrderByCreatedAtDesc(shopId).stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuoteRequest> findBySupplierId(UUID supplierId) {
        return jpa.findBySupplierIdOrderByCreatedAtDesc(supplierId).stream().map(this::toDomain).toList();
    }

    private QuoteRequestJpaEntity toEntity(QuoteRequest r) {
        QuoteRequestJpaEntity e = new QuoteRequestJpaEntity();
        e.setId(r.id());
        e.setReference(r.reference());
        e.setSupplierId(r.supplierId());
        e.setSupplierName(r.supplierName());
        e.setShopId(r.shopId());
        e.setShopName(r.shopName());
        e.setNotes(r.notes());
        e.setStatus(r.status());
        e.setLinesJson(r.linesJson());
        e.setCreatedBy(r.createdBy());
        e.setCreatedAt(r.createdAt());
        return e;
    }

    private QuoteRequest toDomain(QuoteRequestJpaEntity e) {
        return QuoteRequest.builder()
                .id(e.getId()).reference(e.getReference())
                .supplierId(e.getSupplierId()).supplierName(e.getSupplierName())
                .shopId(e.getShopId()).shopName(e.getShopName())
                .notes(e.getNotes()).status(e.getStatus()).linesJson(e.getLinesJson())
                .createdBy(e.getCreatedBy()).createdAt(e.getCreatedAt())
                .build();
    }
}
