package com.paymentplatform.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.*;

@Component
@Order(1)
public class IpWhitelistFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(IpWhitelistFilter.class);

    private final Set<String> allowedIps;
    private final ObjectMapper objectMapper;

    public IpWhitelistFilter(@Value("${app.gateway.allowed-ips:}") String allowedIpsCsv,
                             ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        if (allowedIpsCsv == null || allowedIpsCsv.isBlank()) {
            this.allowedIps = Collections.emptySet();
        } else {
            Set<String> ips = new TreeSet<>();
            Arrays.stream(allowedIpsCsv.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(ips::add);
            this.allowedIps = Collections.unmodifiableSet(ips);
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        if (allowedIps.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Sécurité : la décision se fonde sur le pair TCP (getRemoteAddr), jamais sur
        // X-Forwarded-For dont la première entrée est falsifiable par le client. Derrière
        // notre proxy nginx, le pair est le conteneur nginx (réseau bridge Docker, CIDR
        // 172.16.0.0/12 en dev). La limitation par navigateur reste assurée par le
        // RateLimitFilter (XFF), et le logging garde les deux valeurs.
        String clientIp = request.getRemoteAddr();

        if (!isAllowed(clientIp)) {
            log.warn("Accès refusé pour l'IP {} (XFF {}) sur {}", clientIp,
                    request.getHeader("X-Forwarded-For"), request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("timestamp", Instant.now().toString());
            body.put("status", 403);
            body.put("error", "FORBIDDEN");
            body.put("message", "Accès non autorisé depuis cette adresse IP");
            body.put("path", request.getRequestURI());
            response.getWriter().write(objectMapper.writeValueAsString(body));
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Autorise les IP exactes et les plages CIDR IPv4 (ex. 172.16.0.0/12 pour
     * les réseaux bridge Docker, dont les adresses sont attribuées dynamiquement).
     */
    boolean isAllowed(String clientIp) {
        if (allowedIps.contains(clientIp)) {
            return true;
        }
        byte[] addr = parseIpv4(clientIp);
        if (addr == null) {
            return false;
        }
        for (String entry : allowedIps) {
            int slash = entry.indexOf('/');
            if (slash < 0) {
                continue;
            }
            byte[] net = parseIpv4(entry.substring(0, slash));
            int prefix;
            try {
                prefix = Integer.parseInt(entry.substring(slash + 1));
            } catch (NumberFormatException e) {
                continue;
            }
            if (net == null || prefix < 0 || prefix > 32) {
                continue;
            }
            if (matchesCidr(addr, net, prefix)) {
                return true;
            }
        }
        return false;
    }

    private static byte[] parseIpv4(String ip) {
        String[] parts = ip.split("\\.", -1);
        if (parts.length != 4) {
            return null;
        }
        byte[] out = new byte[4];
        for (int i = 0; i < 4; i++) {
            try {
                int v = Integer.parseInt(parts[i]);
                if (v < 0 || v > 255) {
                    return null;
                }
                out[i] = (byte) v;
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return out;
    }

    private static boolean matchesCidr(byte[] addr, byte[] net, int prefix) {
        int fullBytes = prefix / 8;
        int restBits = prefix % 8;
        for (int i = 0; i < fullBytes; i++) {
            if (addr[i] != net[i]) {
                return false;
            }
        }
        if (restBits > 0) {
            int mask = (0xFF << (8 - restBits)) & 0xFF;
            if ((addr[fullBytes] & mask) != (net[fullBytes] & mask)) {
                return false;
            }
        }
        return true;
    }
}
