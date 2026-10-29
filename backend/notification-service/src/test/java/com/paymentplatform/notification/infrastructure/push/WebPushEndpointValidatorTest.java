package com.paymentplatform.notification.infrastructure.push;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.assertj.core.api.Assertions.assertThat;

class WebPushEndpointValidatorTest {

    private static final String ALLOWED = "fcm.googleapis.com,*.notify.windows.com";

    @Test
    void acceptsHttpsEndpointOnAllowedPublicHost() {
        WebPushEndpointValidator validator = validatorWithAddress("142.250.75.238");

        assertThat(validator.isAllowed("https://fcm.googleapis.com/fcm/send/abc")).isTrue();
    }

    @Test
    void acceptsWildcardSubdomainOnPublicAddress() {
        WebPushEndpointValidator validator = validatorWithAddress("52.113.194.132");

        assertThat(validator.isAllowed("https://wns2-par02p.notify.windows.com/w/?token=abc")).isTrue();
    }

    @Test
    void rejectsHttpScheme() {
        WebPushEndpointValidator validator = validatorWithAddress("142.250.75.238");

        assertThat(validator.isAllowed("http://fcm.googleapis.com/fcm/send/abc")).isFalse();
    }

    @Test
    void rejectsHostOutsideAllowlist() {
        WebPushEndpointValidator validator = validatorWithAddress("142.250.75.238");

        assertThat(validator.isAllowed("https://attacker.example.com/push")).isFalse();
    }

    @Test
    void rejectsWildcardLookalikeHost() {
        WebPushEndpointValidator validator = validatorWithAddress("142.250.75.238");

        assertThat(validator.isAllowed("https://evilnotify.windows.com/w/?token=abc")).isFalse();
    }

    @Test
    void rejectsUserInfoAndNonDefaultPort() {
        WebPushEndpointValidator validator = validatorWithAddress("142.250.75.238");

        assertThat(validator.isAllowed("https://user@fcm.googleapis.com/x")).isFalse();
        assertThat(validator.isAllowed("https://fcm.googleapis.com:8443/x")).isFalse();
    }

    @Test
    void rejectsPrivateLoopbackAndLinkLocalAddresses() throws Exception {
        assertThat(validatorWithAddress("10.0.0.8").isAllowed("https://fcm.googleapis.com/x")).isFalse();
        assertThat(validatorWithAddress("172.20.1.2").isAllowed("https://fcm.googleapis.com/x")).isFalse();
        assertThat(validatorWithAddress("192.168.1.10").isAllowed("https://fcm.googleapis.com/x")).isFalse();
        assertThat(validatorWithAddress("127.0.0.1").isAllowed("https://fcm.googleapis.com/x")).isFalse();
        assertThat(validatorWithAddress("169.254.169.254").isAllowed("https://fcm.googleapis.com/x")).isFalse();
        assertThat(validatorWithAddress("100.64.0.1").isAllowed("https://fcm.googleapis.com/x")).isFalse();
        assertThat(validatorWithAddress("::1").isAllowed("https://fcm.googleapis.com/x")).isFalse();
        assertThat(validatorWithAddress("fd00::1").isAllowed("https://fcm.googleapis.com/x")).isFalse();
    }

    @Test
    void rejectsEndpointWhenDnsResolutionFails() {
        WebPushEndpointValidator validator = new WebPushEndpointValidator(ALLOWED, host -> {
            throw new UnknownHostException(host);
        });

        assertThat(validator.isAllowed("https://fcm.googleapis.com/x")).isFalse();
    }

    @Test
    void rejectsOverlongEndpoint() {
        WebPushEndpointValidator validator = validatorWithAddress("142.250.75.238");
        String endpoint = "https://fcm.googleapis.com/" + "a".repeat(800);

        assertThat(validator.isAllowed(endpoint)).isFalse();
    }

    private WebPushEndpointValidator validatorWithAddress(String address) {
        return new WebPushEndpointValidator(ALLOWED, host -> new InetAddress[]{
                InetAddress.getByName(address)});
    }
}
