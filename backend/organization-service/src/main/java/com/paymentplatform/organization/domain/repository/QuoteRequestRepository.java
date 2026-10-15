package com.paymentplatform.organization.domain.repository;

import com.paymentplatform.organization.domain.model.QuoteRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuoteRequestRepository {
    QuoteRequest save(QuoteRequest request);
    Optional<QuoteRequest> findById(UUID id);
    List<QuoteRequest> findByShopId(UUID shopId);
    List<QuoteRequest> findBySupplierId(UUID supplierId);
}
