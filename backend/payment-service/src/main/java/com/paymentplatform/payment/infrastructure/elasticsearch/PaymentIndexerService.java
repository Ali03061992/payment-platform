package com.paymentplatform.payment.infrastructure.elasticsearch;

import com.paymentplatform.payment.domain.model.Payment;
import com.paymentplatform.payment.infrastructure.http.OrganizationValidationClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PaymentIndexerService {

    private static final Logger log = LoggerFactory.getLogger(PaymentIndexerService.class);

    private final PaymentSearchRepository searchRepository;
    private final OrganizationValidationClient orgClient;

    public PaymentIndexerService(PaymentSearchRepository searchRepository,
                                  OrganizationValidationClient orgClient) {
        this.searchRepository = searchRepository;
        this.orgClient = orgClient;
    }

    public void indexPayment(Payment payment) {
        try {
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
            searchRepository.save(doc);
            log.debug("Indexed payment {} to Elasticsearch", payment.id());
        } catch (Exception e) {
            log.warn("Failed to index payment {} to Elasticsearch: {}", payment.id(), e.getMessage());
        }
    }

    public void removePayment(long paymentId) {
        try {
            searchRepository.deleteById(paymentId);
        } catch (Exception e) {
            log.warn("Failed to remove payment {} from Elasticsearch: {}", paymentId, e.getMessage());
        }
    }

    public Optional<PaymentSearchDocument> findById(long id) {
        return searchRepository.findById(id);
    }
}
