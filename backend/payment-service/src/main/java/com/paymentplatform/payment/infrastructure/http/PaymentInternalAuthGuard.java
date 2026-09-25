package com.paymentplatform.payment.infrastructure.http;

import com.paymentplatform.shared.domain.security.InternalSecretValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** Garde des appels internes vers payment-service (header X-Internal-Token). */
@Component
public class PaymentInternalAuthGuard {

    private final String internalSecret;

    public PaymentInternalAuthGuard(@Value("${app.internal-secret}") String internalSecret,
                                    Environment environment) {
        // B3 : échec au boot si absent ; refus des défauts connus sous profil prod.
        this.internalSecret = InternalSecretValidator.requireValid(internalSecret, environment);
    }

    public boolean isValid(String token) {
        return !internalSecret.isBlank() && internalSecret.equals(token);
    }
}
