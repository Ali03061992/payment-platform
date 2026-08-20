package com.paymentplatform.shared.infrastructure.web;

import org.slf4j.MDC;

/** Corrélation : header X-Correlation-Id propagé (filtre → MDC → logs → réponse). */
public final class CorrelationId {

    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    private static final ThreadLocal<String> HOLDER = new ThreadLocal<>();

    private CorrelationId() {
    }

    public static String get() {
        return HOLDER.get();
    }

    public static void set(String correlationId) {
        HOLDER.set(correlationId);
        MDC.put(MDC_KEY, correlationId);
    }

    public static void clear() {
        HOLDER.remove();
        MDC.remove(MDC_KEY);
    }
}