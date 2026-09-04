package com.paymentplatform.payment.application.usecase;

import com.paymentplatform.shared.domain.exception.NotFoundException;
import com.paymentplatform.payment.application.dto.PaymentNameResolver;
import com.paymentplatform.payment.application.dto.PaymentResponse;
import com.paymentplatform.payment.domain.model.Payment;
import com.paymentplatform.payment.domain.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetPaymentUseCase {

    private final PaymentRepository payments;
    private final PaymentNameResolver nameResolver;

    public GetPaymentUseCase(PaymentRepository payments, PaymentNameResolver nameResolver) {
        this.payments = payments;
        this.nameResolver = nameResolver;
    }

    @Transactional(readOnly = true)
    public PaymentResponse execute(long id) {
        Payment payment = payments.findById(id)
                .orElseThrow(() -> new NotFoundException("Paiement non trouvé : " + id));
        return PaymentResponse.from(payment, nameResolver.toNameResolver());
    }

    @Transactional(readOnly = true)
    public PaymentResponse execute(String reference) {
        Payment payment = payments.findByReference(reference)
                .orElseThrow(() -> new NotFoundException("Paiement non trouvé : " + reference));
        return PaymentResponse.from(payment, nameResolver.toNameResolver());
    }
}
