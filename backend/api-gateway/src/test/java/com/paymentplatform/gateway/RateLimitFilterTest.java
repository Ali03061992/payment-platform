package com.paymentplatform.gateway;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * B2 : le brute-force sur login/register/refresh doit être freiné.
 * Test unitaire pur (pas de contexte Spring) du {@link RateLimitFilter}.
 */
class RateLimitFilterTest {

    private static final String IP = "203.0.113.7";

    private MockHttpServletRequest authRequest(String uri, String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", uri);
        request.setRequestURI(uri);
        request.setRemoteAddr(ip);
        return request;
    }

    private int doFilter(RateLimitFilter filter, MockHttpServletRequest request,
                         MockHttpServletResponse response) throws Exception {
        filter.doFilter(request, response, new MockFilterChain());
        return response.getStatus();
    }

    @Test
    void login_11thRequestInOneMinute_returns429WithRetryAfter() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(120, 10);

        for (int i = 0; i < 10; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            assertThat(doFilter(filter, authRequest("/api/auth/login", IP), response))
                    .isEqualTo(HttpServletResponse.SC_OK);
        }

        MockHttpServletResponse blocked = new MockHttpServletResponse();
        assertThat(doFilter(filter, authRequest("/api/auth/login", IP), blocked)).isEqualTo(429);
        assertThat(blocked.getHeader("Retry-After")).isEqualTo("60");
        assertThat(blocked.getContentType()).contains("application/json");
        assertThat(blocked.getContentAsString()).contains("TOO_MANY_REQUESTS");
    }

    @Test
    void registerAndRefresh_shareTheSameStrictAuthBucket() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(120, 10);

        for (int i = 0; i < 10; i++) {
            doFilter(filter, authRequest("/api/auth/login", IP), new MockHttpServletResponse());
        }

        // Même IP : register et refresh puisent dans le même bucket auth déjà épuisé.
        assertThat(doFilter(filter, authRequest("/api/auth/register", IP), new MockHttpServletResponse()))
                .isEqualTo(429);
        assertThat(doFilter(filter, authRequest("/api/auth/refresh", IP), new MockHttpServletResponse()))
                .isEqualTo(429);
    }

    @Test
    void authResponses_carryRateLimitHeaders() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(120, 10);

        MockHttpServletResponse response = new MockHttpServletResponse();
        doFilter(filter, authRequest("/api/auth/login", IP), response);

        assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("10");
        assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("9");
    }

    @Test
    void generalTraffic_usesSeparateBucketFromAuth() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(120, 10);

        // Épuise le bucket auth.
        for (int i = 0; i < 11; i++) {
            doFilter(filter, authRequest("/api/auth/login", IP), new MockHttpServletResponse());
        }

        // Le trafic API général n'est pas impacté.
        MockHttpServletRequest api = new MockHttpServletRequest("GET", "/api/payments");
        api.setRequestURI("/api/payments");
        api.setRemoteAddr(IP);
        assertThat(doFilter(filter, api, new MockHttpServletResponse()))
                .isEqualTo(HttpServletResponse.SC_OK);
    }

    @Test
    void exhaustedGeneralBucket_doesNotBlockAuth() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(2, 10000);

        for (int i = 0; i < 3; i++) {
            MockHttpServletRequest api = new MockHttpServletRequest("GET", "/api/payments");
            api.setRequestURI("/api/payments");
            api.setRemoteAddr(IP);
            doFilter(filter, api, new MockHttpServletResponse());
        }

        MockHttpServletResponse login = new MockHttpServletResponse();
        assertThat(doFilter(filter, authRequest("/api/auth/login", IP), login))
                .isEqualTo(HttpServletResponse.SC_OK);
    }

    @Test
    void buckets_areIsolatedPerIp() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(120, 2);

        for (int i = 0; i < 3; i++) {
            doFilter(filter, authRequest("/api/auth/login", "10.0.0.1"), new MockHttpServletResponse());
        }
        assertThat(doFilter(filter, authRequest("/api/auth/login", "10.0.0.1"), new MockHttpServletResponse()))
                .isEqualTo(429);

        // Autre IP : budget intact.
        assertThat(doFilter(filter, authRequest("/api/auth/login", "10.0.0.2"), new MockHttpServletResponse()))
                .isEqualTo(HttpServletResponse.SC_OK);
    }

    @Test
    void excludedPaths_bypassRateLimit() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(120, 1);

        // Épuise le bucket auth.
        doFilter(filter, authRequest("/api/auth/login", IP), new MockHttpServletResponse());
        assertThat(doFilter(filter, authRequest("/api/auth/login", IP), new MockHttpServletResponse()))
                .isEqualTo(429);

        // Actuator + preflight CORS restent exclus.
        MockHttpServletRequest actuator = new MockHttpServletRequest("GET", "/actuator/health");
        actuator.setRequestURI("/actuator/health");
        actuator.setRemoteAddr(IP);
        assertThat(doFilter(filter, actuator, new MockHttpServletResponse()))
                .isEqualTo(HttpServletResponse.SC_OK);

        MockHttpServletRequest options = new MockHttpServletRequest("OPTIONS", "/api/auth/login");
        options.setRequestURI("/api/auth/login");
        options.setRemoteAddr(IP);
        assertThat(doFilter(filter, options, new MockHttpServletResponse()))
                .isEqualTo(HttpServletResponse.SC_OK);
    }
}
