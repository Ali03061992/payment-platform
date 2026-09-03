package com.paymentplatform.payment.infrastructure.elasticsearch;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface PaymentSearchRepository extends ElasticsearchRepository<PaymentSearchDocument, Long> {

    List<PaymentSearchDocument> findByShopIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            long shopId, java.time.Instant from, java.time.Instant to);

    List<PaymentSearchDocument> findBySupplierIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            long supplierId, java.time.Instant from, java.time.Instant to);

    List<PaymentSearchDocument> findByShopId(long shopId);

    List<PaymentSearchDocument> findBySupplierId(long supplierId);

    List<PaymentSearchDocument> findByCreatedBy(long createdBy);
}
