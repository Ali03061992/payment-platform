package com.paymentplatform.notification.infrastructure.push;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.UUID;

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

    @Test
    void buildPayload_usesOpenWindowOperation() throws Exception {
        WebPushSender sender = new WebPushSender(null, null, null, false, "mailto:test@example.com");
        UUID orgId = UUID.randomUUID();
        String json = sender.buildPayload("Title", "Body", "tag-1", "/orders/42", orgId);

        JsonNode root = new ObjectMapper().readTree(json);
        JsonNode data = root.at("/notification/data");
        assertThat(data.at("/onActionClick/default/operation").asText()).isEqualTo("openWindow");
        assertThat(data.at("/onActionClick/default/url").asText()).isEqualTo("/orders/42");
        assertThat(data.at("/url").asText()).isEqualTo("/orders/42");
        assertThat(data.at("/organizationId").asText()).isEqualTo(orgId.toString());
        assertThat(root.at("/notification/title").asText()).isEqualTo("Title");
        assertThat(root.at("/notification/body").asText()).isEqualTo("Body");
        assertThat(root.at("/notification/tag").asText()).isEqualTo("tag-1");
    }

    @Test
    void buildPayload_defaultsWhenFieldsNull() throws Exception {
        WebPushSender sender = new WebPushSender(null, null, null, false, "mailto:test@example.com");
        String json = sender.buildPayload(null, null, null, "/dashboard", null);

        JsonNode root = new ObjectMapper().readTree(json);
        assertThat(root.at("/notification/title").asText()).isEqualTo("Notification");
        assertThat(root.at("/notification/body").asText()).isEmpty();
        assertThat(root.at("/notification/tag").asText()).isEqualTo("payment-notification");
        assertThat(root.at("/notification/data/organizationId").asText()).isEmpty();
    }
}
