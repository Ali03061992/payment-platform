package com.paymentplatform.payment.infrastructure.test;

import com.paymentplatform.payment.infrastructure.elasticsearch.PaymentIndexerService;
import com.paymentplatform.payment.infrastructure.http.OrganizationValidationClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Optional;

@org.springframework.context.annotation.Configuration
@Profile("test")
public class TestPaymentBeansConfiguration {
}
