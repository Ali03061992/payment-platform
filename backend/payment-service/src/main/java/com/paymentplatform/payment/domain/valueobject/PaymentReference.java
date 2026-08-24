package com.paymentplatform.payment.domain.valueobject;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

public record PaymentReference(String value) {

    public static PaymentReference generate() {
        long ts = Instant.now().toEpochMilli();
        int rand = ThreadLocalRandom.current().nextInt(100000, 999999);
        return new PaymentReference("PAY-" + ts + "-" + rand);
    }
}
