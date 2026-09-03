package com.paymentplatform.payment.application.usecase;

import com.paymentplatform.payment.application.dto.SearchPaymentsRequest;
import com.paymentplatform.payment.application.dto.SearchPaymentsResponse;
import com.paymentplatform.payment.infrastructure.elasticsearch.PaymentSearchDocument;
import com.paymentplatform.payment.infrastructure.elasticsearch.PaymentSearchRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SearchPaymentsUseCase {

    private final ElasticsearchOperations elasticsearchOperations;
    private final PaymentSearchRepository searchRepository;

    public SearchPaymentsUseCase(ElasticsearchOperations elasticsearchOperations,
                                  PaymentSearchRepository searchRepository) {
        this.elasticsearchOperations = elasticsearchOperations;
        this.searchRepository = searchRepository;
    }

    public SearchPaymentsResponse execute(SearchPaymentsRequest request, Long supplierId) {
        Criteria criteria = new Criteria();

        if (supplierId != null) {
            criteria = criteria.and("supplierId").is(supplierId);
        }

        if (request.shopId() != null) {
            criteria = criteria.and("shopId").is(request.shopId());
        }

        if (request.createdBy() != null) {
            criteria = criteria.and("createdBy").is(request.createdBy());
        }

        if (request.status() != null && !request.status().isBlank()) {
            criteria = criteria.and("status").is(request.status());
        }

        if (request.from() != null || request.to() != null) {
            if (request.from() != null && request.to() != null) {
                criteria = criteria.and("createdAt").between(request.from(), request.to());
            } else if (request.from() != null) {
                criteria = criteria.and("createdAt").greaterThanEqual(request.from());
            } else {
                criteria = criteria.and("createdAt").lessThanEqual(request.to());
            }
        }

        CriteriaQuery query = new CriteriaQuery(criteria, PageRequest.of(request.page(), request.size()));

        SearchHits<PaymentSearchDocument> hits = elasticsearchOperations.search(query, PaymentSearchDocument.class);

        List<SearchPaymentsResponse.PaymentSearchResult> results = hits.getSearchHits().stream()
                .map(hit -> {
                    PaymentSearchDocument doc = hit.getContent();
                    return new SearchPaymentsResponse.PaymentSearchResult(
                            doc.getId(),
                            doc.getReference(),
                            doc.getShopId(),
                            doc.getShopName(),
                            doc.getSupplierId(),
                            doc.getSupplierName(),
                            doc.getCreatedBy(),
                            doc.getCreatedByName(),
                            doc.getAmount(),
                            doc.getCurrency(),
                            doc.getStatus(),
                            doc.getRejectionReason(),
                            doc.getCreatedAt(),
                            doc.getUpdatedAt()
                    );
                })
                .collect(Collectors.toList());

        return new SearchPaymentsResponse(results, hits.getTotalHits(), request.page(), request.size());
    }
}
