package com.paymentplatform.notification.infrastructure.rest;

import com.paymentplatform.notification.domain.model.FcmToken;
import com.paymentplatform.notification.domain.model.FcmTokenRepository;
import com.paymentplatform.shared.infrastructure.security.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FcmTokenControllerTest {

    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OTHER = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Autowired
    private FcmTokenController controller;

    @Autowired
    private FcmTokenRepository repository;

    @Test
    void register_existingTokenOwnedByAnotherUser_returnsConflictAndKeepsOwner() {
        repository.save(new FcmToken(OWNER, UUID.fromString("00000000-0000-0000-0000-000000000010"), "token-owned-by-user-one"));

        var response = controller.registerToken(user(OTHER), Map.of("token", "token-owned-by-user-one"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(repository.findByToken("token-owned-by-user-one").orElseThrow().userId()).isEqualTo(OWNER);
    }

    @Test
    void register_sameUser_touchesExistingToken() {
        repository.save(new FcmToken(OWNER, UUID.fromString("00000000-0000-0000-0000-000000000010"), "token-owned-by-user-one"));

        var response = controller.registerToken(user(OWNER), Map.of("token", "token-owned-by-user-one"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(repository.findByToken("token-owned-by-user-one")).isPresent();
    }

    @Test
    void register_overlongToken_returnsBadRequest() {
        var response = controller.registerToken(user(OWNER), Map.of("token", "a".repeat(513)));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private AuthenticatedUser user(UUID userId) {
        return new AuthenticatedUser(userId, "user", List.of("SHOP_ADMIN"), UUID.randomUUID());
    }
}

