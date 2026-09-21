package com.paymentplatform.shared.infrastructure.web;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ApiErrorTest {

    @Test
    void record_createsInstance() {
        Instant now = Instant.now();
        ApiError error = new ApiError(now, 404, "NOT_FOUND", "Not found", "/api/test");

        assertThat(error.timestamp()).isEqualTo(now);
        assertThat(error.status()).isEqualTo(404);
        assertThat(error.error()).isEqualTo("NOT_FOUND");
        assertThat(error.message()).isEqualTo("Not found");
        assertThat(error.path()).isEqualTo("/api/test");
    }

    @Test
    void record_canBeNullFields() {
        ApiError error = new ApiError(null, 500, null, null, null);
        assertThat(error.timestamp()).isNull();
        assertThat(error.error()).isNull();
    }
}
