package com.paymentplatform.identity.infrastructure.configuration;

import com.paymentplatform.identity.domain.model.UserStatus;
import com.paymentplatform.identity.domain.repository.UserRepository;
import com.paymentplatform.identity.domain.valueobject.Username;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DataInitializerTest {

    @Autowired private UserRepository users;
    @Autowired private PasswordEncoder passwordEncoder;

    private DataInitializer createInitializer() {
        return new DataInitializer(users, passwordEncoder);
    }

    @Test
    void run_createsAdminWhenNotExists() {
        createInitializer().run(null);

        assertThat(users.existsByUsername(Username.of("system.admin"))).isTrue();
    }

    @Test
    void run_doesNotOverwriteExisting() {
        DataInitializer init = createInitializer();
        init.run(null);
        init.run(null);

        assertThat(users.findPage(null, null, null, 0, 100).items().stream()
                .filter(u -> u.username().value().equals("system.admin"))
                .count()).isEqualTo(1);
    }

    @Test
    void run_createsAdminWithCorrectRole() {
        createInitializer().run(null);

        var admin = users.findByUsername(Username.of("system.admin")).orElseThrow();
        assertThat(admin.hasRole(com.paymentplatform.shared.domain.model.RoleCode.SYSTEM_ADMIN)).isTrue();
        assertThat(admin.status()).isEqualTo(UserStatus.ACTIVE);
    }
}
