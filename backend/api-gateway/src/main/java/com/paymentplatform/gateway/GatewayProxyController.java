package com.paymentplatform.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Enumeration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api")
public class GatewayProxyController {

    private static final Logger log = LoggerFactory.getLogger(GatewayProxyController.class);
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
    public ResponseEntity<?> proxyIdentity(HttpServletRequest request,
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
    public ResponseEntity<?> proxyOrganization(HttpServletRequest request,
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
    public ResponseEntity<?> proxyPayment(HttpServletRequest request,
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
    public ResponseEntity<?> proxyNotification(HttpServletRequest request,
                                                 @RequestBody(required = false) byte[] body,
                                                 HttpServletResponse servletResponse) throws Exception {
        String accept = request.getHeader("Accept");
        boolean isSse = accept != null && accept.contains("text/event-stream");

        if (isSse) {
            return proxySse(request, "http", notificationUrl, notificationPort, servletResponse);
        }
        return proxy(request, "http", notificationUrl, notificationPort, body);
    }

    private ResponseEntity<?> proxySse(HttpServletRequest request, String scheme,
                                        String host, String port,
                                        HttpServletResponse servletResponse) throws Exception {
        String query = request.getQueryString();
        String targetUri = scheme + "://" + host + ":" + port + request.getRequestURI()
                + (query != null ? "?" + query : "");

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(targetUri))
                .GET();

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null) {
            builder.header("Authorization", authHeader);
        }
        builder.header("Accept", "text/event-stream");

        HttpResponse<InputStream> response = httpClient.send(builder.build(),
                HttpResponse.BodyHandlers.ofInputStream());

        servletResponse.setStatus(response.statusCode());
        servletResponse.setContentType("text/event-stream");
        servletResponse.setCharacterEncoding("UTF-8");
        servletResponse.setHeader("Cache-Control", "no-cache");
        servletResponse.setHeader("Connection", "keep-alive");

        try (OutputStream os = servletResponse.getOutputStream();
             InputStream is = response.body()) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
                os.flush();
            }
        } catch (Exception e) {
            log.debug("SSE stream closed: {}", e.getMessage());
        }

        return ResponseEntity.ok().build();
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
