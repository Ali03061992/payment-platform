package com.paymentplatform.notification.application;

/** Politique d'admission des endpoints Web Push (HTTPS, hote autorise, adresse publique). */
public interface WebPushEndpointPolicy {

    boolean isAllowed(String endpoint);
}
