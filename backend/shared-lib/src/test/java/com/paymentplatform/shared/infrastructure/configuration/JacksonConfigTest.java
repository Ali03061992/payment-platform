package com.paymentplatform.shared.infrastructure.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class JacksonConfigTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void objectMapper_isConfigured() {
        assertThat(objectMapper).isNotNull();
    }

    @Test
    void objectMapper_serializesInstantAsIso() throws Exception {
        Instant instant = Instant.parse("2026-01-15T10:30:00Z");
        Map<String, Object> data = Map.of("date", instant);
        String json = objectMapper.writeValueAsString(data);
        assertThat(json).contains("2026-01-15T10:30:00Z");
    }

    @Test
    void objectMapper_doesNotWriteDatesAsTimestamps() {
        assertThat(objectMapper.isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)).isFalse();
    }
}
