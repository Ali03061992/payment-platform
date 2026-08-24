package com.paymentplatform.payment.application.usecase;

import com.paymentplatform.shared.domain.exception.NotFoundException;
import com.paymentplatform.payment.application.dto.PaymentResponse;
import com.paymentplatform.payment.domain.model.Payment;
import com.paymentplatform.payment.domain.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetPaymentUseCase {

    private final PaymentRepository payments;

    public GetPaymentUseCase(PaymentRepository payments) {
        this.payments = payments;
    }

    @Transactional(readOnly = true)
    public PaymentResponse execute(long id) {
        Payment payment = payments.findById(id)
                .orElseThrow(() -> new NotFoundException("Paiement non trouvé : " + id));
        return PaymentResponse.from(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse execute(String reference) {
        Payment payment = payments.findByReference(reference)
                .orElseThrow(() -> new NotFoundException("Paiement non trouvé : " + reference));
        return PaymentResponse.from(payment);
    }
}
