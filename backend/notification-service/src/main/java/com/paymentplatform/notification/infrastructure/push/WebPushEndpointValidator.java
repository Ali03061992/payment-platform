package com.paymentplatform.notification.infrastructure.push;

import com.paymentplatform.notification.application.WebPushEndpointPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Valide les endpoints Web Push avant enregistrement et avant envoi :
 * HTTPS obligatoire, port 443, hote dans la liste blanche, adresses DNS publiques.
 * Empeche l'enregistrement d'URLs internes (SSRF) et la resolution DNS rebinding.
 */
@Component
public class WebPushEndpointValidator implements WebPushEndpointPolicy {

    private static final Logger log = LoggerFactory.getLogger(WebPushEndpointValidator.class);

    static final String DEFAULT_ALLOWED_HOSTS =
            "fcm.googleapis.com,updates.push.services.mozilla.com,"
                    + "push.services.mozilla.com,web.push.apple.com,*.notify.windows.com";

    private final List<String> allowedHosts;
    private final HostResolver resolver;

    @Autowired
    public WebPushEndpointValidator(
            @Value("${app.webpush.allowed-hosts:" + DEFAULT_ALLOWED_HOSTS + "}") String allowedHosts) {
        this(allowedHosts, InetAddress::getAllByName);
    }

    WebPushEndpointValidator(String allowedHosts, HostResolver resolver) {
        this.allowedHosts = Arrays.stream(allowedHosts.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .toList();
        this.resolver = resolver;
    }

    @Override
    public boolean isAllowed(String endpoint) {
        if (endpoint == null || endpoint.isBlank() || endpoint.length() > 768) {
            return false;
        }
        URI uri;
        try {
            uri = new URI(endpoint.trim());
        } catch (URISyntaxException e) {
            log.debug("Web Push endpoint rejected: invalid URI");
            return false;
        }
        if (!"https".equalsIgnoreCase(uri.getScheme())
                || uri.getUserInfo() != null
                || uri.getHost() == null
                || (uri.getPort() != -1 && uri.getPort() != 443)) {
            log.debug("Web Push endpoint rejected: HTTPS on port 443 required");
            return false;
        }
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        if (!isAllowedHost(host)) {
            log.debug("Web Push endpoint rejected: host not allowed");
            return false;
        }
        try {
            InetAddress[] addresses = resolver.resolve(host);
            if (addresses == null || addresses.length == 0) {
                log.debug("Web Push endpoint rejected: no DNS address for {}", host);
                return false;
            }
            for (InetAddress address : addresses) {
                if (!isPublic(address)) {
                    log.warn("Web Push endpoint rejected: non-public address for {}", host);
                    return false;
                }
            }
            return true;
        } catch (UnknownHostException e) {
            log.debug("Web Push endpoint rejected: unknown host {}", host);
            return false;
        }
    }

    private boolean isAllowedHost(String host) {
        for (String allowed : allowedHosts) {
            if (allowed.startsWith("*.")) {
                if (host.endsWith(allowed.substring(1))) {
                    return true;
                }
            } else if (host.equals(allowed)) {
                return true;
            }
        }
        return false;
    }

    private boolean isPublic(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress()
                || address.isLinkLocalAddress() || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return false;
        }
        byte[] bytes = address.getAddress();
        if (bytes.length == 4) {
            return isPublicIpv4(bytes, 0);
        }
        if (bytes.length == 16) {
            int first = bytes[0] & 0xff;
            if ((first & 0xfe) == 0xfc) {
                return false;
            }
            boolean ipv4Mapped = true;
            for (int i = 0; i < 10; i++) {
                if (bytes[i] != 0) {
                    ipv4Mapped = false;
                    break;
                }
            }
            if (ipv4Mapped && (bytes[10] & 0xff) == 0xff && (bytes[11] & 0xff) == 0xff) {
                return isPublicIpv4(bytes, 12);
            }
        }
        return true;
    }

    private boolean isPublicIpv4(byte[] bytes, int offset) {
        int first = bytes[offset] & 0xff;
        int second = bytes[offset + 1] & 0xff;
        return first != 0
                && first != 10
                && first != 127
                && !(first == 100 && second >= 64 && second <= 127)
                && !(first == 169 && second == 254)
                && !(first == 172 && second >= 16 && second <= 31)
                && !(first == 192 && (second == 0 || second == 168))
                && !(first == 198 && (second == 18 || second == 19 || second == 51))
                && !(first == 203 && second == 0)
                && first < 224;
    }

    @FunctionalInterface
    interface HostResolver {
        InetAddress[] resolve(String host) throws UnknownHostException;
    }
}
