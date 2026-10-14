package com.paymentplatform.organization.infrastructure.persistence;

import com.paymentplatform.organization.domain.model.OrderQuote;
import com.paymentplatform.organization.domain.repository.OrderQuoteRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
public class JpaOrderQuoteRepository implements OrderQuoteRepository {

    private final OrderQuoteJpaRepository jpa;

    public JpaOrderQuoteRepository(OrderQuoteJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional
    public OrderQuote save(OrderQuote quote) {
        return toDomain(jpa.save(toEntity(quote)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderQuote> findByOrderId(UUID orderId) {
        return jpa.findByOrderIdOrderByVersionAsc(orderId).stream().map(this::toDomain).toList();
    }

    private OrderQuoteJpaEntity toEntity(OrderQuote q) {
        OrderQuoteJpaEntity e = new OrderQuoteJpaEntity();
        e.setId(q.id());
        e.setOrderId(q.orderId());
        e.setOrderReference(q.orderReference());
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

    private OrderQuote toDomain(OrderQuoteJpaEntity e) {
        return OrderQuote.builder()
                .id(e.getId()).orderId(e.getOrderId()).orderReference(e.getOrderReference())
                .supplierId(e.getSupplierId()).version(e.getVersion())
                .globalDiscount(e.getGlobalDiscount()).subtotal(e.getSubtotal())
                .taxAmount(e.getTaxAmount()).total(e.getTotal()).stampAmount(e.getStampAmount())
                .currency(e.getCurrency()).linesJson(e.getLinesJson())
                .createdBy(e.getCreatedBy()).createdAt(e.getCreatedAt())
                .build();
    }
}
