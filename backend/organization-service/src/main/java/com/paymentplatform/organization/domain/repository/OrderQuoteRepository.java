package com.paymentplatform.organization.domain.repository;

import com.paymentplatform.organization.domain.model.OrderQuote;

import java.util.List;
import java.util.UUID;

public interface OrderQuoteRepository {
    OrderQuote save(OrderQuote quote);
    List<OrderQuote> findByOrderId(UUID orderId);
}
