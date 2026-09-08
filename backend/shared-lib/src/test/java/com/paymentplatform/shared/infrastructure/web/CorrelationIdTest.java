package com.paymentplatform.shared.infrastructure.web;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdTest {

    @Test
    void setAndGet_returnsSetValue() {
        CorrelationId.set("test-id-123");
        assertThat(CorrelationId.get()).isEqualTo("test-id-123");
        CorrelationId.clear();
    }

    @Test
    void set_putsToMDC() {
        CorrelationId.set("mdc-test");
        assertThat(MDC.get(CorrelationId.MDC_KEY)).isEqualTo("mdc-test");
        CorrelationId.clear();
    }

    @Test
    void clear_removesFromHolderAndMDC() {
        CorrelationId.set("to-clear");
        CorrelationId.clear();
        assertThat(CorrelationId.get()).isNull();
        assertThat(MDC.get(CorrelationId.MDC_KEY)).isNull();
    }

    @Test
    void get_returnsNull_whenNotSet() {
        CorrelationId.clear();
        assertThat(CorrelationId.get()).isNull();
    }

    @Test
    void constants_haveExpectedValues() {
        assertThat(CorrelationId.HEADER).isEqualTo("X-Correlation-Id");
        assertThat(CorrelationId.MDC_KEY).isEqualTo("correlationId");
    }
}
