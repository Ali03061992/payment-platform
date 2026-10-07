package com.paymentplatform.identity.application.usecase;

import com.paymentplatform.identity.application.dto.UpdatePreferencesRequest;
import com.paymentplatform.identity.domain.model.User;
import com.paymentplatform.identity.domain.repository.UserRepository;
import com.paymentplatform.identity.domain.valueobject.Email;
import com.paymentplatform.identity.domain.valueobject.PasswordHash;
import com.paymentplatform.identity.domain.valueobject.PhoneNumber;
import com.paymentplatform.identity.domain.valueobject.Username;
import com.paymentplatform.shared.domain.model.OrganizationId;
import com.paymentplatform.shared.domain.model.RoleCode;
import com.paymentplatform.shared.domain.model.UserId;
import com.paymentplatform.shared.infrastructure.security.AuthenticatedUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserPreferencesUseCaseH2Test {

    @Autowired private UserPreferencesUseCase preferences;
    @Autowired private UserRepository users;
    @Autowired private PasswordEncoder passwordEncoder;

    private UUID userId;

    @BeforeEach
    void setUp() {
        User user = User.create(new UserId(null), Username.of("prefs.user"),
                Email.of("prefs@test.com"), PasswordHash.of(passwordEncoder.encode("pass")),
                "Prefs", "Test", new PhoneNumber(null),
                OrganizationId.of(UUID.fromString("00000000-0000-0000-0000-000000000005")), RoleCode.SHOP_AGENT);
        users.save(user);
        userId = users.findByUsername(Username.of("prefs.user")).orElseThrow().id().value();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new AuthenticatedUser(userId, "prefs.user", List.of("SHOP_AGENT"),
                                UUID.fromString("00000000-0000-0000-0000-000000000005")),
                        null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void updatePreferences_fullUpdate_persisted() {
        var response = preferences.updatePreferences(
                new UpdatePreferencesRequest("en", "#ff0000", "#00ff00", true));

        assertThat(response.preferredLang()).isEqualTo("en");
        assertThat(response.accentColor1()).isEqualTo("#ff0000");
        assertThat(response.accentColor2()).isEqualTo("#00ff00");
        assertThat(response.tourSeen()).isTrue();

        var reloaded = users.findById(UserId.of(userId)).orElseThrow();
        assertThat(reloaded.preferredLang()).isEqualTo("en");
        assertThat(reloaded.tourSeen()).isTrue();
    }

    @Test
    void updatePreferences_partialUpdate_keepsOtherValues() {
        var response = preferences.updatePreferences(
                new UpdatePreferencesRequest("en", null, null, null));

        assertThat(response.preferredLang()).isEqualTo("en");
        assertThat(response.accentColor1()).isEqualTo("#0284c7");
        assertThat(response.accentColor2()).isEqualTo("#e63946");
        assertThat(response.tourSeen()).isFalse();
    }

    @Test
    void updatePreferences_tourSeenOnly_marksSeen() {
        var response = preferences.updatePreferences(
                new UpdatePreferencesRequest(null, null, null, true));

        assertThat(response.tourSeen()).isTrue();
        assertThat(response.preferredLang()).isEqualTo("fr");
    }
}
