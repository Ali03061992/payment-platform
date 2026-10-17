package com.paymentplatform.notification.infrastructure.push;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WebPushSenderTest {

    @Test
    void sanitizeUrl_nullOrBlank_fallsBackToDashboard() {
        assertThat(WebPushSender.sanitizeUrl(null)).isEqualTo("/dashboard");
        assertThat(WebPushSender.sanitizeUrl("")).isEqualTo("/dashboard");
        assertThat(WebPushSender.sanitizeUrl("   ")).isEqualTo("/dashboard");
    }

    @Test
    void sanitizeUrl_rejectsDangerousSchemes() {
        assertThat(WebPushSender.sanitizeUrl("javascript:alert(1)")).isEqualTo("/dashboard");
        assertThat(WebPushSender.sanitizeUrl("JaVaScRiPt:alert(1)")).isEqualTo("/dashboard");
        assertThat(WebPushSender.sanitizeUrl("data:text/html,<script>")).isEqualTo("/dashboard");
        assertThat(WebPushSender.sanitizeUrl("vbscript:msgbox")).isEqualTo("/dashboard");
        assertThat(WebPushSender.sanitizeUrl("//evil.example.com")).isEqualTo("/dashboard");
        assertThat(WebPushSender.sanitizeUrl("ftp://example.com")).isEqualTo("/dashboard");
    }

    @Test
    void sanitizeUrl_rejectsOverlongUrls() {
        assertThat(WebPushSender.sanitizeUrl("/".repeat(3000))).isEqualTo("/dashboard");
    }

    @Test
    void sanitizeUrl_acceptsRelativeDeepLinks() {
        assertThat(WebPushSender.sanitizeUrl("/dashboard/payments")).isEqualTo("/dashboard/payments");
        assertThat(WebPushSender.sanitizeUrl("/dashboard/disputes?tab=2")).isEqualTo("/dashboard/disputes?tab=2");
    }

    @Test
    void sanitizeUrl_acceptsHttpsAbsoluteUrls() {
        assertThat(WebPushSender.sanitizeUrl("https://app.example.com/orders/42"))
                .isEqualTo("https://app.example.com/orders/42");
    }
}
