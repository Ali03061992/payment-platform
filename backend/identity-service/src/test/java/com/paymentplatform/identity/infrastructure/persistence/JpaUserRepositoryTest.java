package com.paymentplatform.identity.infrastructure.persistence;

import com.paymentplatform.identity.domain.model.User;
import com.paymentplatform.identity.domain.valueobject.Email;
import com.paymentplatform.identity.domain.valueobject.PasswordHash;
import com.paymentplatform.identity.domain.valueobject.PhoneNumber;
import com.paymentplatform.identity.domain.valueobject.Username;
import com.paymentplatform.shared.domain.model.OrganizationId;
import com.paymentplatform.shared.domain.model.RoleCode;
import com.paymentplatform.shared.domain.model.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

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
        savedUser = repository.save(User.create(new UserId(null),
                Username.of("testuser"), Email.of("test@example.com"),
                PasswordHash.of(passwordEncoder.encode("Test@1")),
                "Test", "User", new PhoneNumber("55123456"),
                OrganizationId.of(UUID.fromString("00000000-0000-0000-0000-000000000010")), RoleCode.SHOP_AGENT));
    }

    @Test
    void findById_existingUser() {
        var found = repository.findById(new UserId(savedUser.id().value()));
        assertThat(found).isPresent();
        assertThat(found.get().username().value()).isEqualTo("testuser");
    }

    @Test
    void findById_nonExisting_returnsEmpty() {
        assertThat(repository.findById(new UserId(UUID.fromString("00000000-0000-0000-0000-000000099999")))).isEmpty();
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
        repository.save(User.create(new UserId(null),
                Username.of("user2"), Email.of("user2@example.com"),
                PasswordHash.of(passwordEncoder.encode("Test@1")),
                "User", "Two", new PhoneNumber(null),
                OrganizationId.of(UUID.fromString("00000000-0000-0000-0000-000000000010")), RoleCode.SHOP_ADMIN));

        List<User> users = repository.findByOrganizationId(OrganizationId.of(UUID.fromString("00000000-0000-0000-0000-000000000010")));
        assertThat(users).hasSize(2);
    }

    @Test
    void findByOrganizationId_emptyWhenNone() {
        List<User> users = repository.findByOrganizationId(OrganizationId.of(UUID.fromString("00000000-0000-0000-0000-000000000999")));
        assertThat(users).isEmpty();
    }

    @Test
    void findByOrganizationIdAndRole_filtersCorrectly() {
        repository.save(User.create(new UserId(null),
                Username.of("admin"), Email.of("admin@example.com"),
                PasswordHash.of(passwordEncoder.encode("Test@1")),
                "Admin", "User", new PhoneNumber(null),
                OrganizationId.of(UUID.fromString("00000000-0000-0000-0000-000000000010")), RoleCode.SHOP_ADMIN));

        List<User> agents = repository.findByOrganizationIdAndRole(OrganizationId.of(UUID.fromString("00000000-0000-0000-0000-000000000010")), RoleCode.SHOP_AGENT);
        assertThat(agents).hasSize(1);
        assertThat(agents.get(0).username().value()).isEqualTo("testuser");
    }

    @Test
    void findPage_returnsPagedResults() {
        long before = repository.findPage(null, null, null, 0, 100).totalElements();
        repository.save(User.create(new UserId(null),
                Username.of("other"), Email.of("other@example.com"),
                PasswordHash.of(passwordEncoder.encode("Test@1")),
                "Other", "User", new PhoneNumber(null),
                OrganizationId.of(UUID.fromString("00000000-0000-0000-0000-000000000020")), RoleCode.SUPPLIER_ADMIN));

        var page = repository.findPage(null, null, null, 0, 100);
        assertThat(page.totalElements()).isEqualTo(before + 1);
        assertThat(page.items()).hasSize((int) (before + 1));
    }

    @Test
    void findPage_filtersByRoleAndStatus() {
        var page = repository.findPage(
                OrganizationId.of(UUID.fromString("00000000-0000-0000-0000-000000000010")),
                "ACTIVE", RoleCode.SHOP_AGENT, 0, 100);
        assertThat(page.items()).hasSize(1);
        assertThat(page.items().get(0).username().value()).isEqualTo("testuser");
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
