package com.paymentplatform.payment.infrastructure.test;

import java.util.UUID;

import com.paymentplatform.payment.infrastructure.elasticsearch.PaymentIndexerService;
import com.paymentplatform.payment.infrastructure.http.OrganizationValidationClient;
import com.paymentplatform.payment.domain.model.Payment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Primary
@Profile("test")
public class TestPaymentIndexerService extends PaymentIndexerService {

    public TestPaymentIndexerService() {
        super(null, null, null);
    }

    @Override
    public void indexPayment(Payment payment) {
    }

    @Override
    public void removePayment(UUID paymentId) {
    }

    @Override
    public int reindexAll() {
        return 0;
    }
}
