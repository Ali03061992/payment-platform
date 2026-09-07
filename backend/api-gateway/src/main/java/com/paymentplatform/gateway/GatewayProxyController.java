package com.paymentplatform.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Enumeration;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api")
public class GatewayProxyController {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${IDENTITY_SERVICE_URL:localhost}")
    private String identityUrl;
    @Value("${IDENTITY_SERVICE_PORT:8082}")
    private String identityPort;
    @Value("${ORGANIZATION_SERVICE_URL:localhost}")
    private String organizationUrl;
    @Value("${ORGANIZATION_SERVICE_PORT:8083}")
    private String organizationPort;
    @Value("${PAYMENT_SERVICE_URL:localhost}")
    private String paymentUrl;
    @Value("${PAYMENT_SERVICE_PORT:8084}")
    private String paymentPort;
    @Value("${NOTIFICATION_SERVICE_URL:localhost}")
    private String notificationUrl;
    @Value("${NOTIFICATION_SERVICE_PORT:8085}")
    private String notificationPort;

    @RequestMapping(value = {"/auth/**", "/users/**", "/suppliers/{supplierId}/agents/**", "/internal/**"}, method = {
            org.springframework.web.bind.annotation.RequestMethod.GET,
            org.springframework.web.bind.annotation.RequestMethod.POST,
            org.springframework.web.bind.annotation.RequestMethod.PUT,
            org.springframework.web.bind.annotation.RequestMethod.PATCH,
            org.springframework.web.bind.annotation.RequestMethod.DELETE
    })
    public ResponseEntity<byte[]> proxyIdentity(HttpServletRequest request,
                                                @RequestBody(required = false) byte[] body) throws Exception {
        return proxy(request, "http", identityUrl, identityPort, body);
    }

    @RequestMapping(value = {"/admin/**", "/organizations/**",
            "/supplier/catalog/**", "/orders/**", "/balances/**",
            "/suppliers/{supplierId}/products/**", "/suppliers/{supplierId}/movements/**",
            "/suppliers/{supplierId}/stocks/**", "/suppliers/{supplierId}/optimization/**",
            "/categories/**"}, method = {
            org.springframework.web.bind.annotation.RequestMethod.GET,
            org.springframework.web.bind.annotation.RequestMethod.POST,
            org.springframework.web.bind.annotation.RequestMethod.PUT,
            org.springframework.web.bind.annotation.RequestMethod.PATCH,
            org.springframework.web.bind.annotation.RequestMethod.DELETE
    })
    public ResponseEntity<byte[]> proxyOrganization(HttpServletRequest request,
                                                     @RequestBody(required = false) byte[] body) throws Exception {
        return proxy(request, "http", organizationUrl, organizationPort, body);
    }

    @RequestMapping(value = {"/payments/**"}, method = {
            org.springframework.web.bind.annotation.RequestMethod.GET,
            org.springframework.web.bind.annotation.RequestMethod.POST,
            org.springframework.web.bind.annotation.RequestMethod.PUT,
            org.springframework.web.bind.annotation.RequestMethod.PATCH,
            org.springframework.web.bind.annotation.RequestMethod.DELETE
    })
    public ResponseEntity<byte[]> proxyPayment(HttpServletRequest request,
                                                @RequestBody(required = false) byte[] body) throws Exception {
        return proxy(request, "http", paymentUrl, paymentPort, body);
    }

    @RequestMapping(value = {"/notifications/**"}, method = {
            org.springframework.web.bind.annotation.RequestMethod.GET,
            org.springframework.web.bind.annotation.RequestMethod.POST,
            org.springframework.web.bind.annotation.RequestMethod.PUT,
            org.springframework.web.bind.annotation.RequestMethod.PATCH,
            org.springframework.web.bind.annotation.RequestMethod.DELETE
    })
    public ResponseEntity<byte[]> proxyNotification(HttpServletRequest request,
                                                     @RequestBody(required = false) byte[] body) throws Exception {
        return proxy(request, "http", notificationUrl, notificationPort, body);
    }

    private ResponseEntity<byte[]> proxy(HttpServletRequest request, String scheme,
                                         String host, String port, byte[] body) throws Exception {
        String query = request.getQueryString();
        String targetUri = scheme + "://" + host + ":" + port + request.getRequestURI()
                + (query != null ? "?" + query : "");

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(targetUri))
                .method(request.getMethod(), body != null
                        ? HttpRequest.BodyPublishers.ofByteArray(body)
                        : HttpRequest.BodyPublishers.noBody());

        String contentType = request.getContentType();
        if (contentType != null) {
            builder.header("Content-Type", contentType);
        }

        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            String lower = name.toLowerCase();
            if (lower.startsWith("authorization") || lower.startsWith("accept")
                    || lower.startsWith("x-") || lower.equals("user-agent")) {
                builder.header(name, request.getHeader(name));
            }
        }

        HttpResponse<byte[]> response = httpClient.send(builder.build(),
                HttpResponse.BodyHandlers.ofByteArray());

        String respContentType = response.headers().firstValue("Content-Type").orElse("application/octet-stream");
        return ResponseEntity.status(response.statusCode())
                .header("Content-Type", respContentType)
                .body(response.body());
    }
}
