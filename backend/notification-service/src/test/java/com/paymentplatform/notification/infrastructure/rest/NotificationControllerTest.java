package com.paymentplatform.notification.infrastructure.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentplatform.notification.domain.model.Notification;
import com.paymentplatform.notification.domain.model.NotificationRepository;
import com.paymentplatform.shared.infrastructure.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotificationControllerTest {

    @Autowired private WebApplicationContext wac;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private NotificationRepository notifications;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        notifications.deleteAll();
    }

    private static UsernamePasswordAuthenticationToken auth(UUID userId, String username, List<String> perms, UUID orgId) {
        AuthenticatedUser principal = new AuthenticatedUser(userId, username, perms, orgId);
        var authorities = perms.stream().map(SimpleGrantedAuthority::new).toList();
        return new UsernamePasswordAuthenticationToken(principal, null, authorities);
    }

    @Test
    void getNotifications_userWithOrg_returnsOrgNotifications() throws Exception {
        notifications.save(new Notification(UUID.fromString("00000000-0000-0000-0000-000000000000"), UUID.fromString("00000000-0000-0000-0000-000000000010"), "TYPE1", "msg1", "ORDER", "1"));
        notifications.save(new Notification(UUID.fromString("00000000-0000-0000-0000-000000000000"), UUID.fromString("00000000-0000-0000-0000-000000000010"), "TYPE2", "msg2", "ORDER", "2"));
        notifications.save(new Notification(UUID.fromString("00000000-0000-0000-0000-000000000000"), UUID.fromString("00000000-0000-0000-0000-000000000020"), "TYPE3", "msg3", "ORDER", "3"));

        mockMvc.perform(get("/api/notifications")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(
                                auth(UUID.fromString("00000000-0000-0000-0000-000000000001"), "user", List.of("SHOP_ADMIN"), UUID.fromString("00000000-0000-0000-0000-000000000010")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getNotifications_userWithoutOrg_returnsUserNotifications() throws Exception {
        notifications.save(new Notification(UUID.fromString("00000000-0000-0000-0000-000000000005"), null, "TYPE1", "msg1", "PAYMENT", "1"));
        notifications.save(new Notification(UUID.fromString("00000000-0000-0000-0000-000000000005"), null, "TYPE2", "msg2", "PAYMENT", "2"));
        notifications.save(new Notification(UUID.fromString("00000000-0000-0000-0000-000000000007"), null, "TYPE3", "msg3", "PAYMENT", "3"));

        mockMvc.perform(get("/api/notifications")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(
                                auth(UUID.fromString("00000000-0000-0000-0000-000000000005"), "user", List.of("SYSTEM_ADMIN"), null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void unreadCount_userWithOrg() throws Exception {
        notifications.save(new Notification(UUID.fromString("00000000-0000-0000-0000-000000000000"), UUID.fromString("00000000-0000-0000-0000-000000000010"), "TYPE1", "msg1", null, null));
        notifications.save(new Notification(UUID.fromString("00000000-0000-0000-0000-000000000000"), UUID.fromString("00000000-0000-0000-0000-000000000010"), "TYPE2", "msg2", null, null));
        Notification read = notifications.save(new Notification(UUID.fromString("00000000-0000-0000-0000-000000000000"), UUID.fromString("00000000-0000-0000-0000-000000000010"), "TYPE3", "msg3", null, null));
        read.markAsRead();
        notifications.save(read);

        mockMvc.perform(get("/api/notifications/unread-count")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(
                                auth(UUID.fromString("00000000-0000-0000-0000-000000000001"), "user", List.of("SHOP_ADMIN"), UUID.fromString("00000000-0000-0000-0000-000000000010")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2));
    }

    @Test
    void unreadCount_userWithoutOrg() throws Exception {
        notifications.save(new Notification(UUID.fromString("00000000-0000-0000-0000-000000000005"), null, "TYPE1", "msg1", null, null));
        notifications.save(new Notification(UUID.fromString("00000000-0000-0000-0000-000000000005"), null, "TYPE2", "msg2", null, null));

        mockMvc.perform(get("/api/notifications/unread-count")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(
                                auth(UUID.fromString("00000000-0000-0000-0000-000000000005"), "user", List.of("SYSTEM_ADMIN"), null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2));
    }

    @Test
    void markAllAsRead_userWithOrg() throws Exception {
        notifications.save(new Notification(UUID.fromString("00000000-0000-0000-0000-000000000000"), UUID.fromString("00000000-0000-0000-0000-000000000010"), "TYPE1", "msg1", null, null));
        notifications.save(new Notification(UUID.fromString("00000000-0000-0000-0000-000000000000"), UUID.fromString("00000000-0000-0000-0000-000000000010"), "TYPE2", "msg2", null, null));

        mockMvc.perform(post("/api/notifications/read-all")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(
                                auth(UUID.fromString("00000000-0000-0000-0000-000000000001"), "user", List.of("SHOP_ADMIN"), UUID.fromString("00000000-0000-0000-0000-000000000010")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updated").value(2));
    }

    @Test
    void markAllAsRead_userWithoutOrg() throws Exception {
        notifications.save(new Notification(UUID.fromString("00000000-0000-0000-0000-000000000005"), null, "TYPE1", "msg1", null, null));
        notifications.save(new Notification(UUID.fromString("00000000-0000-0000-0000-000000000005"), null, "TYPE2", "msg2", null, null));

        mockMvc.perform(post("/api/notifications/read-all")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(
                                auth(UUID.fromString("00000000-0000-0000-0000-000000000005"), "user", List.of("SYSTEM_ADMIN"), null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updated").value(2));
    }

    @Test
    void markAsRead_validNotification() throws Exception {
        Notification n = notifications.save(new Notification(UUID.fromString("00000000-0000-0000-0000-000000000005"), null, "TYPE1", "msg1", null, null));

        mockMvc.perform(post("/api/notifications/" + n.id() + "/read")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(
                                auth(UUID.fromString("00000000-0000-0000-0000-000000000005"), "user", List.of("SYSTEM_ADMIN"), null))))
                .andExpect(status().isNoContent());

        var found = notifications.findById(n.id()).orElseThrow();
        assertThat(found.readStatus()).isEqualTo("READ");
    }

    @Test
    void markAsRead_notOwned_returns403() throws Exception {
        Notification n = notifications.save(new Notification(UUID.fromString("00000000-0000-0000-0000-000000000099"), null, "TYPE1", "msg1", null, null));

        mockMvc.perform(post("/api/notifications/" + n.id() + "/read")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(
                                auth(UUID.fromString("00000000-0000-0000-0000-000000000005"), "user", List.of("SYSTEM_ADMIN"), null))))
                .andExpect(status().isForbidden());
    }

    @Test
    void markAsRead_nonexistent_returns500() throws Exception {
        mockMvc.perform(post("/api/notifications/99999/read")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(
                                auth(UUID.fromString("00000000-0000-0000-0000-000000000005"), "user", List.of("SYSTEM_ADMIN"), null))))
                .andExpect(status().is5xxServerError());
    }
}
