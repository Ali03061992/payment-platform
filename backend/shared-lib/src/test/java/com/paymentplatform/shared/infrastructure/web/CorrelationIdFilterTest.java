package com.paymentplatform.shared.infrastructure.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void doFilterInternal_withExistingHeader_propagatesIt() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Correlation-Id", "my-id-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, (req, res) -> {
            assertThat(CorrelationId.get()).isEqualTo("my-id-123");
        });

        assertThat(response.getHeader("X-Correlation-Id")).isEqualTo("my-id-123");
        assertThat(CorrelationId.get()).isNull();
    }

    @Test
    void doFilterInternal_noHeader_generatesNewId() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, (req, res) -> {
            assertThat(CorrelationId.get()).isNotBlank();
        });

        assertThat(response.getHeader("X-Correlation-Id")).isNotBlank();
    }

    @Test
    void doFilterInternal_blankHeader_generatesNewId() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Correlation-Id", "   ");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, (req, res) -> {
            assertThat(CorrelationId.get()).isNotBlank();
        });

        assertThat(response.getHeader("X-Correlation-Id")).isNotBlank();
    }

    @Test
    void doFilterInternal_clearsCorrelationIdAfterChain() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, (req, res) -> {
            assertThat(CorrelationId.get()).isNotNull();
        });

        assertThat(CorrelationId.get()).isNull();
    }
}
