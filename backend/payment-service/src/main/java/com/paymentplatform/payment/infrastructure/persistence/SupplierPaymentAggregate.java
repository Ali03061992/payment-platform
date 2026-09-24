package com.paymentplatform.payment.infrastructure.persistence;

import com.paymentplatform.payment.domain.model.PaymentStatus;

import java.math.BigDecimal;

/**
 * B5 : projection Spring Data de la requête d'agrégats par statut.
 */
public interface SupplierPaymentAggregate {

    PaymentStatus getStatus();

    long getCnt();

    BigDecimal getTotal();
}
