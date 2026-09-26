package com.paymentplatform.identity.application.usecase;

import com.paymentplatform.identity.application.dto.RegisterRequest;
import com.paymentplatform.identity.domain.model.User;
import com.paymentplatform.identity.domain.repository.UserRepository;
import com.paymentplatform.identity.domain.valueobject.Email;
import com.paymentplatform.identity.domain.valueobject.PasswordHash;
import com.paymentplatform.identity.domain.valueobject.PhoneNumber;
import com.paymentplatform.identity.domain.valueobject.Username;
import com.paymentplatform.shared.domain.exception.ConflictException;
import com.paymentplatform.shared.domain.model.RoleCode;
import com.paymentplatform.shared.domain.model.UserId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
/**
 * Tests de RegisterUseCaseH2Test.
 * Perimetre : cas d'usage/service RegisterUseCase sur base H2.
 * Moyens : contexte SpringBootTest, profil "test" (H2).
 */

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RegisterUseCaseH2Test {

    @Autowired private RegisterUseCase register;
    @Autowired private UserRepository users;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void register_validRequest_createsUser() {
        var request = new RegisterRequest("reg.newuser", "reg.new@example.com", "Password123",
                "Jean", "Dupont", "+21620123456", "SUPPLIER_ADMIN");
        var response = register.register(request);

        assertThat(response.username()).isEqualTo("reg.newuser");
        assertThat(response.email()).isEqualTo("reg.new@example.com");
        assertThat(response.firstName()).isEqualTo("Jean");
        assertThat(response.lastName()).isEqualTo("Dupont");
        assertThat(response.roles()).containsExactly("SUPPLIER_ADMIN");
        // M5 : auto-inscription => compte désactivé en attente de validation admin.
        assertThat(response.status()).isEqualTo("DISABLED");
        assertThat(response.organizationId()).isNull();
    }

    @Test
    void register_selfRegistered_cannotLoginUntilActivated() {
        var request = new RegisterRequest("reg.pending", "reg.pending@example.com", "Password123",
                "Jean", "Dupont", "+21620123456", "SUPPLIER_ADMIN");
        register.register(request);

        User stored = users.findByUsername(Username.of("reg.pending")).orElseThrow();
        assertThat(stored.isActive()).isFalse();
        assertThat(stored.organizationId()).isNull();
    }

    @Test
    void register_duplicateUsername_throwsConflict() {
        User existing = User.create(new UserId(null), Username.of("reg.taken"),
                Email.of("reg.first@example.com"), PasswordHash.of(passwordEncoder.encode("pass")),
                "A", "B", new PhoneNumber(null), null, RoleCode.SHOP_AGENT);
        users.save(existing);

        var request = new RegisterRequest("reg.taken", "reg.second@example.com", "Password123",
                "C", "D", null, "SHOP_AGENT");
        assertThatThrownBy(() -> register.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Nom d'utilisateur déjà utilisé");
    }

    @Test
    void register_duplicateEmail_throwsConflict() {
        User existing = User.create(new UserId(null), Username.of("reg.user1"),
                Email.of("reg.dup@example.com"), PasswordHash.of(passwordEncoder.encode("pass")),
                "A", "B", new PhoneNumber(null), null, RoleCode.SHOP_AGENT);
        users.save(existing);

        var request = new RegisterRequest("reg.user2", "reg.dup@example.com", "Password123",
                "C", "D", null, "SHOP_AGENT");
        assertThatThrownBy(() -> register.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Adresse email déjà utilisée");
    }

    @Test
    void register_invalidRole_throwsConflict() {
        var request = new RegisterRequest("reg.user", "reg.u@x.com", "Password123",
                "A", "B", null, "SYSTEM_ADMIN");
        assertThatThrownBy(() -> register.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("non autorisé");
    }

    @Test
    void register_shopAgent_succeeds() {
        var request = new RegisterRequest("reg.shop.agent", "reg.sa@x.com", "Password123",
                "Shop", "Agent", null, "SHOP_AGENT");
        var response = register.register(request);
        assertThat(response.roles()).containsExactly("SHOP_AGENT");
    }

    @Test
    void register_withNullPhone_succeeds() {
        var request = new RegisterRequest("reg.nophone", "reg.np@x.com", "Password123",
                "No", "Phone", null, "SUPPLIER_AGENT");
        var response = register.register(request);
        assertThat(response.phone()).isNull();
    }
}
