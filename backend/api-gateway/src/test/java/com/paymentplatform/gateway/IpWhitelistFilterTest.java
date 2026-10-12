package com.paymentplatform.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Allowlist IP : IP exactes + plages CIDR (réseaux bridge Docker dynamiques).
 * JUnit pur (aucun contexte Spring).
 */
class IpWhitelistFilterTest {

    private IpWhitelistFilter filter(String csv) {
        return new IpWhitelistFilter(csv, new ObjectMapper());
    }

    @Test
    void exactIp_allowed() {
        var f = filter("127.0.0.1,0:0:0:0:0:0:0:1");
        assertThat(f.isAllowed("127.0.0.1")).isTrue();
        assertThat(f.isAllowed("10.0.0.5")).isFalse();
    }

    @Test
    void cidrRange_coversDockerBridge() {
        var f = filter("127.0.0.1,172.16.0.0/12");
        assertThat(f.isAllowed("172.18.0.1")).isTrue();
        assertThat(f.isAllowed("172.31.255.254")).isTrue();
        assertThat(f.isAllowed("172.16.0.1")).isTrue();
        assertThat(f.isAllowed("172.32.0.1")).isFalse();
        assertThat(f.isAllowed("192.168.1.10")).isFalse();
        assertThat(f.isAllowed("127.0.0.1")).isTrue();
    }

    @Test
    void cidrBoundaries_respected() {
        var f = filter("10.0.1.0/24");
        assertThat(f.isAllowed("10.0.1.50")).isTrue();
        assertThat(f.isAllowed("10.0.2.50")).isFalse();
    }

    @Test
    void invalidEntries_ignoredSafely() {
        var f = filter("not-an-ip,10.0.0.0/33,10.0.0.0/abc,127.0.0.1");
        assertThat(f.isAllowed("10.9.9.9")).isFalse();
        assertThat(f.isAllowed("127.0.0.1")).isTrue();
    }

    @Test
    void emptyAllowlist_matchesNothing() {
        // Liste vide : isAllowed() est faux, mais doFilterInternal laisse tout
        // passer (comportement existant conservé).
        var f = filter("");
        assertThat(f.isAllowed("203.0.113.9")).isFalse();
    }

    private org.springframework.mock.web.MockHttpServletRequest proxiedRequest(String remoteAddr, String xff) {
        var request = new org.springframework.mock.web.MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr(remoteAddr);
        if (xff != null) {
            request.addHeader("X-Forwarded-For", xff);
        }
        return request;
    }

    private int doFilter(IpWhitelistFilter f, org.springframework.mock.web.MockHttpServletRequest request)
            throws Exception {
        var response = new org.springframework.mock.web.MockHttpServletResponse();
        f.doFilter(request, response, new org.springframework.mock.web.MockFilterChain());
        return response.getStatus();
    }

    @Test
    void spoofedXff_ignoredForDecision() throws Exception {
        // Navigateur via nginx : pair = conteneur proxy (autorisé), XFF falsifiable ignoré.
        var f = filter("127.0.0.1,172.16.0.0/12");
        assertThat(doFilter(f, proxiedRequest("172.18.0.5", "203.0.113.9"))).isEqualTo(200);
    }

    @Test
    void disallowedPeer_rejectedEvenWithoutXff() throws Exception {
        var f = filter("127.0.0.1,172.16.0.0/12");
        assertThat(doFilter(f, proxiedRequest("192.168.1.50", null))).isEqualTo(403);
    }

    @Test
    void allowedPeerWithoutXff_passes() throws Exception {
        var f = filter("127.0.0.1,172.16.0.0/12");
        assertThat(doFilter(f, proxiedRequest("127.0.0.1", null))).isEqualTo(200);
    }
}
