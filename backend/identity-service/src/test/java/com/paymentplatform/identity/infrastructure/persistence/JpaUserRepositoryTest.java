package com.paymentplatform.identity.infrastructure.persistence;

import com.paymentplatform.shared.domain.model.OrganizationId;
import com.paymentplatform.shared.domain.model.RoleCode;
import com.paymentplatform.shared.domain.model.UserId;
import com.paymentplatform.identity.domain.model.User;
import com.paymentplatform.identity.domain.repository.UserRepository;
import com.paymentplatform.identity.domain.valueobject.Email;
import com.paymentplatform.identity.domain.valueobject.PasswordHash;
import com.paymentplatform.identity.domain.valueobject.PhoneNumber;
import com.paymentplatform.identity.domain.valueobject.Username;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class JpaUserRepositoryTest {

    @Autowired private JpaUserRepository repository;
    @Autowired private PasswordEncoder passwordEncoder;

    private User savedUser;

    @BeforeEach
    void setUp() {
        savedUser = repository.save(User.create(new UserId(0),
                Username.of("testuser"), Email.of("test@example.com"),
                PasswordHash.of(passwordEncoder.encode("Test@1")),
                "Test", "User", new PhoneNumber("55123456"),
                OrganizationId.of(10), RoleCode.SHOP_AGENT));
    }

    @Test
    void findById_existingUser() {
        var found = repository.findById(new UserId(savedUser.id().value()));
        assertThat(found).isPresent();
        assertThat(found.get().username().value()).isEqualTo("testuser");
    }

    @Test
    void findById_nonExisting_returnsEmpty() {
        assertThat(repository.findById(new UserId(99999))).isEmpty();
    }

    @Test
    void findByUsername_existing() {
        var found = repository.findByUsername(Username.of("testuser"));
        assertThat(found).isPresent();
        assertThat(found.get().email().value()).isEqualTo("test@example.com");
    }

    @Test
    void findByUsername_nonExisting() {
        assertThat(repository.findByUsername(Username.of("ghost"))).isEmpty();
    }

    @Test
    void findByEmail_existing() {
        var found = repository.findByEmail(Email.of("test@example.com"));
        assertThat(found).isPresent();
    }

    @Test
    void existsByUsername_true() {
        assertThat(repository.existsByUsername(Username.of("testuser"))).isTrue();
    }

    @Test
    void existsByUsername_false() {
        assertThat(repository.existsByUsername(Username.of("nobody"))).isFalse();
    }

    @Test
    void existsByEmail_true() {
        assertThat(repository.existsByEmail(Email.of("test@example.com"))).isTrue();
    }

    @Test
    void existsByEmail_false() {
        assertThat(repository.existsByEmail(Email.of("nobody@example.com"))).isFalse();
    }

    @Test
    void findByOrganizationId() {
        repository.save(User.create(new UserId(0),
                Username.of("user2"), Email.of("user2@example.com"),
                PasswordHash.of(passwordEncoder.encode("Test@1")),
                "User", "Two", new PhoneNumber(null),
                OrganizationId.of(10), RoleCode.SHOP_ADMIN));

        List<User> users = repository.findByOrganizationId(OrganizationId.of(10));
        assertThat(users).hasSize(2);
    }

    @Test
    void findByOrganizationId_emptyWhenNone() {
        List<User> users = repository.findByOrganizationId(OrganizationId.of(999));
        assertThat(users).isEmpty();
    }

    @Test
    void findByOrganizationIdAndRole_filtersCorrectly() {
        repository.save(User.create(new UserId(0),
                Username.of("admin"), Email.of("admin@example.com"),
                PasswordHash.of(passwordEncoder.encode("Test@1")),
                "Admin", "User", new PhoneNumber(null),
                OrganizationId.of(10), RoleCode.SHOP_ADMIN));

        List<User> agents = repository.findByOrganizationIdAndRole(OrganizationId.of(10), RoleCode.SHOP_AGENT);
        assertThat(agents).hasSize(1);
        assertThat(agents.get(0).username().value()).isEqualTo("testuser");
    }

    @Test
    void findAll_returnsAll() {
        long before = repository.findAll().size();
        repository.save(User.create(new UserId(0),
                Username.of("other"), Email.of("other@example.com"),
                PasswordHash.of(passwordEncoder.encode("Test@1")),
                "Other", "User", new PhoneNumber(null),
                OrganizationId.of(20), RoleCode.SUPPLIER_ADMIN));

        List<User> all = repository.findAll();
        assertThat(all).hasSize((int) (before + 1));
    }

    @Test
    void save_updatesExisting() {
        User updated = User.reconstruct(savedUser.id(),
                savedUser.username(), savedUser.email(), savedUser.password(),
                "Updated", "Name", savedUser.phone(), savedUser.organizationId(),
                savedUser.status(), savedUser.roles(), savedUser.version(),
                savedUser.createdAt(), savedUser.updatedAt());
        repository.save(updated);

        var found = repository.findById(new UserId(savedUser.id().value()));
        assertThat(found.get().firstName()).isEqualTo("Updated");
    }
}
