package com.paymentplatform.organization.domain.repository;

import com.paymentplatform.organization.domain.model.SupplierQuote;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SupplierQuoteRepository {
    SupplierQuote save(SupplierQuote quote);
    Optional<SupplierQuote> findById(UUID id);
    List<SupplierQuote> findByRequestId(UUID requestId);
}
