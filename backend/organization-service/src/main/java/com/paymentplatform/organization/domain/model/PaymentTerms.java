package com.paymentplatform.organization.domain.model;

import java.time.LocalDate;

public enum PaymentTerms {
    IMMEDIATE(0),
    NET_15(15),
    NET_30(30),
    NET_60(60);

    private final int days;

    PaymentTerms(int days) {
        this.days = days;
    }

    public int getDays() {
        return days;
    }

    public LocalDate computeDueDate(LocalDate baseDate) {
        return baseDate.plusDays(days);
    }
}
