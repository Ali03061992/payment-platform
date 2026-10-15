package com.paymentplatform.organization.application.usecase;

import com.paymentplatform.organization.application.dto.QuoteRequestResponse;
import com.paymentplatform.organization.application.dto.SupplierQuoteResponse;
import com.paymentplatform.organization.domain.repository.QuoteRequestRepository;
import com.paymentplatform.organization.domain.repository.SupplierQuoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Demandes de devis visibles : boutique → les siennes ; fournisseur → celles
 * qui lui sont adressées ; admin → tout (avec filtres optionnels).
 */
@Service
public class ListQuoteRequestsUseCase {

    private final QuoteRequestRepository requests;
    private final SupplierQuoteRepository quotes;

    public ListQuoteRequestsUseCase(QuoteRequestRepository requests, SupplierQuoteRepository quotes) {
        this.requests = requests;
        this.quotes = quotes;
    }

    @Transactional(readOnly = true)
    public List<QuoteRequestResponse> forShop(UUID shopId) {
        return requests.findByShopId(shopId).stream().map(this::withQuotes).toList();
    }

    @Transactional(readOnly = true)
    public List<QuoteRequestResponse> forSupplier(UUID supplierId) {
        return requests.findBySupplierId(supplierId).stream().map(this::withQuotes).toList();
    }

    private QuoteRequestResponse withQuotes(com.paymentplatform.organization.domain.model.QuoteRequest r) {
        List<SupplierQuoteResponse> qs = quotes.findByRequestId(r.id()).stream()
                .map(SupplierQuoteResponse::from).toList();
        return QuoteRequestResponse.from(r, qs);
    }
}
