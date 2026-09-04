package com.paymentplatform.payment.infrastructure.elasticsearch;

import com.paymentplatform.payment.domain.model.Payment;
import com.paymentplatform.payment.domain.repository.PaymentRepository;
import com.paymentplatform.payment.infrastructure.http.OrganizationValidationClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.annotation.PostConstruct;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.stereotype.Service;

@Service
public class PaymentIndexerService {

    private static final Logger log = LoggerFactory.getLogger(PaymentIndexerService.class);
    private static final String INDEX_NAME = "payments";

    private final ElasticsearchOperations elasticsearchOperations;
    private final OrganizationValidationClient orgClient;
    private final PaymentRepository payments;

    public PaymentIndexerService(ElasticsearchOperations elasticsearchOperations,
                                  OrganizationValidationClient orgClient,
                                  PaymentRepository payments) {
        this.elasticsearchOperations = elasticsearchOperations;
        this.orgClient = orgClient;
        this.payments = payments;
    }

    @PostConstruct
    public void init() {
        ensureIndexExists();
    }

    public void indexPayment(Payment payment) {
        try {
            ensureIndexExists();

            String shopName = orgClient.getOrganizationName(payment.shopId()).orElse("Shop " + payment.shopId());
            String supplierName = orgClient.getOrganizationName(payment.supplierId()).orElse("Supplier " + payment.supplierId());
            String agentName = orgClient.getUserName(payment.createdBy()).orElse("User " + payment.createdBy());

            PaymentSearchDocument doc = new PaymentSearchDocument(
                    payment.id(),
                    payment.reference().value(),
                    payment.shopId(),
                    shopName,
                    payment.supplierId(),
                    supplierName,
                    payment.createdBy(),
                    agentName,
                    payment.money().amount(),
                    payment.money().currency(),
                    payment.status().name(),
                    payment.rejectionReason() != null ? payment.rejectionReason().value() : null,
                    payment.createdAt(),
                    payment.updatedAt()
            );

            IndexQuery indexQuery = new IndexQueryBuilder()
                    .withId(String.valueOf(payment.id()))
                    .withObject(doc)
                    .build();

            elasticsearchOperations.index(indexQuery, IndexCoordinates.of(INDEX_NAME));
            log.debug("Indexed payment {} to Elasticsearch", payment.id());
        } catch (Exception e) {
            log.warn("Failed to index payment {} to Elasticsearch: {}", payment.id(), e.getMessage());
        }
    }

    public void removePayment(long paymentId) {
        try {
            elasticsearchOperations.delete(String.valueOf(paymentId), IndexCoordinates.of(INDEX_NAME));
        } catch (Exception e) {
            log.warn("Failed to remove payment {} from Elasticsearch: {}", paymentId, e.getMessage());
        }
    }

    private void ensureIndexExists() {
        try {
            IndexOperations indexOps = elasticsearchOperations.indexOps(IndexCoordinates.of(INDEX_NAME));
            if (!indexOps.exists()) {
                indexOps.create();
            }
        } catch (Exception e) {
            log.warn("Failed to check/create Elasticsearch index: {}", e.getMessage());
        }
    }

    public int reindexAll() {
        ensureIndexExists();
        var allPayments = payments.findAll();
        int indexed = 0;
        for (Payment payment : allPayments) {
            try {
                indexPayment(payment);
                indexed++;
            } catch (Exception e) {
                log.warn("Failed to reindex payment {}: {}", payment.id(), e.getMessage());
            }
        }
        log.info("Reindexed {} payments to Elasticsearch", indexed);
        return indexed;
    }
}
